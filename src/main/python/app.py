from flask import Flask, request, jsonify
# 🔻 recommend_books.py에 정의된 추천 함수를 가져옵니다.
# (예시로 함수 이름이 run_recommendation 이라고 가정했습니다)
from recommend_books import run_recommendation 

app = Flask(__name__)

@app.route('/', methods=['GET'])
def health_check():
    return jsonify({"status": "ok", "message": "Flask AI Recommendation Server is Running!"})

@app.route('/api/v1/ai/recommend', methods=['POST'])
def recommend_books():
    # 1. Spring Boot에서 전달한 JSON 요청 바디
    request_data = request.get_json() or {}
    
    try:
        # 2. recommend_books.py의 알고리즘 함수 실행
        # (request_data를 파라미터로 넘겨 실제 추천 도서 리스트를 전달받음)
        recommended_books = run_recommendation(request_data)
        
        # 3. DTO 포맷(books: [...])에 맞춰 응답
        return jsonify({
            "books": recommended_books
        }), 200

    except Exception as e:
        # 예외 처리 발생 시 백엔드가 원인을 알 수 있도록 500 에러 반환
        return jsonify({
            "error": str(e),
            "message": "AI 추천 처리 중 오류가 발생했습니다."
        }), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)