from pathlib import Path
from typing import Any, Dict, List, Set, Union
import json
import logging
import pandas as pd
from flask import Flask, request, jsonify

# =========================================================
# 0. 기본 설정 및 Flask 초기화
# =========================================================

app = Flask(__name__)

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 엑셀 파일 이름
BOOK_FILE_NAME = "책데이터-완-.csv.xlsx"

# 메모리에 저장될 전역 도서 데이터
books_df = None

# 🌟 엑셀 파일 열 이름 (책 제목 컬럼명 추가!)
ISBN_COL = "ISBN"
TITLE_COL = "제목"  # 💡 엑셀 헤더가 '책제목', '제목' 등이라면 해당 이름으로 수정해 주세요!
CHAR_COUNT_COL = "글자수"
GENRE_COL = "장르"
LOAN_COL = "최근 1년간 대출건수"

# 허용 장르
VALID_GENRES = {
    "소설",
    "수필",
    "기록문학",
}

# 추천 점수 배점
MAX_TIME_SCORE = 60.0
MAX_GENRE_SCORE = 30.0
MAX_POPULARITY_SCORE = 10.0

# 대출정보가 없는 책의 인기도점수
MISSING_POPULARITY_SCORE = 2.5

# 최종 추천 권수
DEFAULT_TOP_N = 5


# =========================================================
# 1. 서버 시작 시 데이터 인메모리 로드 (Flask 서버용)
# =========================================================
def initialize_data():
    global books_df
    file_path = Path(__file__).with_name(BOOK_FILE_NAME)

    logger.info(f"🔥 [AI Server] 도서 데이터 로드 및 전처리 시작... 경로: {file_path}")

    try:
        raw_books = load_book_data(file_path=file_path)
        books_df = calculate_popularity_score(books=raw_books)
        logger.info(f"✅ [AI Server] 도서 데이터 준비 완료: {len(books_df)}권 확보")
    except Exception as e:
        logger.error(f"❌ [AI Server] 데이터 초기화 중 에러 발생: {e}", exc_info=True)


# =========================================================
# 2. 도서 데이터 불러오기 및 전처리
# =========================================================
def load_book_data(file_path: Union[str, Path]) -> pd.DataFrame:
    file_path = Path(file_path)

    if not file_path.exists():
        raise FileNotFoundError(f"도서 데이터 파일을 찾을 수 없습니다: {file_path}")

    books = pd.read_excel(
        file_path,
        dtype={
            ISBN_COL: "string",
            TITLE_COL: "string", # 🌟 제목 컬럼 문자열 타입 지정
        },
    )

    # 🌟 필수 컬럼 체크 목록에 TITLE_COL 추가
    required_columns = {
        ISBN_COL,
        TITLE_COL,
        CHAR_COUNT_COL,
        GENRE_COL,
        LOAN_COL,
    }

    missing_columns = required_columns - set(books.columns)

    if missing_columns:
        raise ValueError(
            f"엑셀 파일에 다음 필수 열이 없습니다: {sorted(missing_columns)}"
        )

    # ISBN 정리 (숫자가 아닌 문자 제거)
    books[ISBN_COL] = (
        books[ISBN_COL]
        .astype("string")
        .str.replace(r"\D", "", regex=True)
    )

    # 🌟 제목 정리 (공백 제거)
    books[TITLE_COL] = books[TITLE_COL].astype("string").str.strip()

    # 글자 수 및 대출건수 수치형 변환
    books[CHAR_COUNT_COL] = pd.to_numeric(
        books[CHAR_COUNT_COL], errors="coerce"
    )
    books[LOAN_COL] = pd.to_numeric(
        books[LOAN_COL], errors="coerce"
    )

    # 장르 정리
    books[GENRE_COL] = books[GENRE_COL].astype("string").str.strip()

    # 유효 데이터 필터링 (제목이 없는 경우도 유효에서 제외)
    valid_rows = (
        books[ISBN_COL].str.fullmatch(r"\d{13}", na=False)
        & books[TITLE_COL].notna()
        & (books[TITLE_COL] != "")
        & books[CHAR_COUNT_COL].notna()
        & (books[CHAR_COUNT_COL] > 0)
        & books[GENRE_COL].isin(VALID_GENRES)
    )

    books = books.loc[valid_rows].copy()

    if books.empty:
        raise ValueError("추천에 사용할 수 있는 유효한 도서가 없습니다.")

    books = books.drop_duplicates(subset=[ISBN_COL], keep="first")
    books = books.reset_index(drop=True)

    return books


# =========================================================
# 3. 사용자 선호 장르 검증
# =========================================================
def normalize_preferred_genres(preferred_genres: Any) -> Set[str]:
    if isinstance(preferred_genres, str):
        preferred_genres = [preferred_genres]

    if not isinstance(preferred_genres, (list, tuple, set)):
        raise ValueError("preferred_genres는 장르 목록이어야 합니다.")

    cleaned_genres = {
        str(genre).strip()
        for genre in preferred_genres
        if str(genre).strip()
    }

    if not cleaned_genres:
        raise ValueError("선호 장르를 한 개 이상 선택해야 합니다.")

    invalid_genres = cleaned_genres - VALID_GENRES

    if invalid_genres:
        raise ValueError(
            f"허용되지 않은 장르가 포함되어 있습니다: {sorted(invalid_genres)}"
        )

    return cleaned_genres


# =========================================================
# 4. 장르별 인기도 점수 계산
# =========================================================
def calculate_popularity_score(books: pd.DataFrame) -> pd.DataFrame:
    result = books.copy()
    result["popularity_score"] = MISSING_POPULARITY_SCORE

    for genre, genre_group in result.groupby(GENRE_COL):
        valid_loans = genre_group[LOAN_COL].dropna()
        valid_count = len(valid_loans)

        if valid_count <= 1:
            continue

        loan_rank = valid_loans.rank(method="average", ascending=True)

        popularity_score = (
            (loan_rank - 1) / (valid_count - 1) * MAX_POPULARITY_SCORE
        )

        result.loc[popularity_score.index, "popularity_score"] = popularity_score

    return result


# =========================================================
# 5. 완독 가능한 책 필터링 및 점수 계산
# =========================================================
def filter_readable_books(
    books: pd.DataFrame, user_cpm: float, one_way_minutes: float
) -> pd.DataFrame:
    readable_characters = user_cpm * one_way_minutes
    candidates = books.loc[
        books[CHAR_COUNT_COL] <= readable_characters
    ].copy()
    return candidates


def calculate_recommendation_scores(
    candidates: pd.DataFrame,
    user_cpm: float,
    one_way_minutes: float,
    preferred_genres: Set[str],
) -> pd.DataFrame:
    result = candidates.copy()

    if result.empty:
        return result

    result["estimated_reading_minutes"] = result[CHAR_COUNT_COL] / user_cpm

    time_ratio = result["estimated_reading_minutes"] / one_way_minutes
    result["time_score"] = (
        time_ratio.clip(lower=0, upper=1) * MAX_TIME_SCORE
    )

    result["is_preferred_genre"] = result[GENRE_COL].isin(preferred_genres)
    result["genre_score"] = (
        result["is_preferred_genre"].astype(float) * MAX_GENRE_SCORE
    )

    result["raw_score"] = (
        result["time_score"]
        + result["genre_score"]
        + result["popularity_score"]
    ).clip(lower=0, upper=100)

    return result


# =========================================================
# 6. 정렬 및 상위 도서 선택
# =========================================================
def sort_books_by_score(books: pd.DataFrame) -> pd.DataFrame:
    if books.empty:
        return books.copy()

    return books.sort_values(
        by=["raw_score", "time_score", "popularity_score", ISBN_COL],
        ascending=[False, False, False, True],
    )


def select_top_books(
    scored_books: pd.DataFrame, top_n: int = DEFAULT_TOP_N
) -> pd.DataFrame:
    if scored_books.empty:
        return scored_books.copy()

    preferred_books = scored_books.loc[scored_books["is_preferred_genre"]].copy()
    non_preferred_books = scored_books.loc[~scored_books["is_preferred_genre"]].copy()

    preferred_books = sort_books_by_score(preferred_books)
    non_preferred_books = sort_books_by_score(non_preferred_books)

    selected_preferred = preferred_books.head(top_n)
    remaining_count = top_n - len(selected_preferred)

    if remaining_count > 0:
        selected_non_preferred = non_preferred_books.head(remaining_count)
        selected_books = pd.concat(
            [selected_preferred, selected_non_preferred], ignore_index=False
        )
    else:
        selected_books = selected_preferred

    return selected_books


# =========================================================
# 7. 최종 추천 로직
# =========================================================
def recommend_books(
    books: pd.DataFrame,
    user_cpm: float,
    one_way_minutes: float,
    preferred_genres: Any,
    top_n: int = DEFAULT_TOP_N,
) -> List[Dict[str, Union[str, int, float]]]:

    user_cpm = float(user_cpm)
    one_way_minutes = float(one_way_minutes)
    preferred_genres_set = normalize_preferred_genres(preferred_genres)

    candidates = filter_readable_books(
        books=books, user_cpm=user_cpm, one_way_minutes=one_way_minutes
    )

    if candidates.empty:
        return []

    scored_books = calculate_recommendation_scores(
        candidates=candidates,
        user_cpm=user_cpm,
        one_way_minutes=one_way_minutes,
        preferred_genres=preferred_genres_set,
    )

    selected_books = select_top_books(scored_books=scored_books, top_n=top_n)

    # 🌟 반환 딕셔너리에 "title" 추가!
    recommendations = [
        {
            "isbn": str(row[ISBN_COL]),
            "title": str(row[TITLE_COL]), # 💡 책 제목 포함
            "estimated_time_minutes": round(
                float(row["estimated_reading_minutes"]), 1
            ),
            "score": int(row["raw_score"]),
        }
        for _, row in selected_books.iterrows()
    ]

    return recommendations


# =========================================================
# 8. Flask API 라우트 (Spring Boot 백엔드 통신용)
# =========================================================
@app.route("/api/v1/ai/recommend", methods=["POST"])
def recommend_books_api():
    logger.info("🚀 [AI Server] /api/v1/ai/recommend 요청 도착!")

    # 1. books_df 상태 점검
    if books_df is None:
        logger.error("❌ [AI Server Error] books_df가 None입니다. 엑셀 로딩에 실패했습니다.")
        return jsonify({"error": "서버 데이터 초기화 실패", "details": "books_df is None"}), 500

    # 2. Request JSON 데이터 파싱 점검
    request_data = request.get_json(silent=True)
    logger.info(f"📥 [AI Server Request Data] 수신된 raw JSON: {request_data}")

    if request_data is None:
        logger.error("❌ [AI Server Error] JSON 데이터를 읽을 수 없습니다. (Content-Type 확인 필요)")
        return jsonify({"error": "요청 데이터가 없거나 JSON 형식이 올바르지 않습니다."}), 400

    try:
        # 3. 안전한 파라미터 추출
        one_way_minutes = float(request_data.get("duration") or request_data.get("one_way_minutes") or 40)
        user_cpm = float(request_data.get("userCpm") or request_data.get("user_cpm") or 950)
        
        preferred_genres = request_data.get("preferredGenres") or request_data.get("preferred_genres") or ["소설"]
        if isinstance(preferred_genres, str):
            preferred_genres = [preferred_genres]

        logger.info(f"📊 [AI Server Parsed] duration={one_way_minutes}, cpm={user_cpm}, genres={preferred_genres}")

        # 4. 추천 로직 실행
        recommendations = recommend_books(
            books=books_df,
            user_cpm=user_cpm,
            one_way_minutes=one_way_minutes,
            preferred_genres=preferred_genres,
            top_n=DEFAULT_TOP_N,
        )

        logger.info(f"✅ [AI Server Success] 추천 결과 {len(recommendations)}권 생성 완료!")
        return jsonify({"books": recommendations}), 200

    except Exception as e:
        # Render 로그에 완벽한 에러 추적 스택트레이스를 찍도록 방어 코드 작성
        import traceback
        error_trace = traceback.format_exc()
        logger.error(f"❌ [AI Server Fatal Error]\n{error_trace}")
        
        return jsonify({
            "error": str(e),
            "message": "AI 추천 계산 처리 중 내부 에러가 발생했습니다."
        }), 500


# =========================================================
# 9. 서버 실행
# =========================================================
if __name__ == "__main__":
    initialize_data()
    logger.info("🔥 [AI Server] 오도독 추천 서버 구동 (포트 5000)...")
    app.run(port=5000, debug=False)