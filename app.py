# from flask import Flask, request, jsonify
# from flask_cors import CORS
# from flask_socketio import SocketIO, emit
# import time
#
# app = Flask(__name__)
# CORS(app)
# # 개발 편의상 모든 오리진 허용. 배포 시에는 허용 도메인만 지정 권장.
# socketio = SocketIO(app, cors_allowed_origins="*")
#
# # 데모용 인메모리 저장소 (서버 재시작하면 초기화됨)
# USERS = {}      # token -> name
# MESSAGES = []   # [{ts, user, text}]
#
# def now_ms() -> int:
#     """현재 시각(ms). 안드로이드에서 Date(ts)로 바로 표시 가능."""
#     return int(time.time() * 1000)
#
# # ──────────────────────────────────────────────────────────────────────
# # 헬스체크: 네트워크/포트/방화벽 이슈와 라우팅 이슈를 빠르게 구분하기 위함
# # ──────────────────────────────────────────────────────────────────────
# @app.get("/health")
# def health():
#     return jsonify(status="ok"), 200
#
# # ──────────────────────────────────────────────────────────────────────
# # 로그인: 닉네임을 받아 간단 토큰 발급 (보안 목적 아님, 데모용)
# # ──────────────────────────────────────────────────────────────────────
# @app.post("/login")
# def login():
#     # ❗ request.json이 None이면 .get()에서 예외 → silent=True로 방어
#     data = request.get_json(silent=True) or {}
#     name = (data.get("name") or "guest").strip() or "guest"
#     token = f"t_{int(time.time())}_{name}"
#     USERS[token] = name
#     return jsonify(token=token, name=name), 200
#
# # ──────────────────────────────────────────────────────────────────────
# # 메시지 목록: 최근 N개만 반환 (기본 50)
# # ──────────────────────────────────────────────────────────────────────
# @app.get("/messages")
# def list_messages():
#     # ❗ 잘못된 쿼리 값이 들어와도 죽지 않게 안전 파싱
#     try:
#         N = int(request.args.get("limit", 50))
#     except Exception:
#         N = 50
#     if N <= 0:
#         N = 1
#     return jsonify(messages=MESSAGES[-N:]), 200
#
# # ──────────────────────────────────────────────────────────────────────
# # 메시지 전송: 토큰 인증 → 저장 → 실시간 브로드캐스트 → 201 Created
# # ──────────────────────────────────────────────────────────────────────
# @app.post("/messages")
# def send_message():
#     # 1) Authorization 헤더에서 토큰 추출 (대소문자/공백 변형에 안전)
#     auth = (request.headers.get("Authorization", "") or "").strip()
#     if auth.lower().startswith("bearer "):
#         token = auth[7:].strip()   # "Bearer " 이후를 토큰으로
#     else:
#         token = auth
#
#     if token not in USERS:
#         return jsonify(error="unauthorized"), 401
#
#     # 2) 본문 안전 파싱
#     data = request.get_json(silent=True) or {}
#     text = (data.get("text") or "").strip()
#     if not text:
#         return jsonify(error="text required"), 400
#
#     # 3) 메시지 생성/저장
#     msg = {"ts": now_ms(), "user": USERS[token], "text": text}
#     MESSAGES.append(msg)
#
#     # 4) 실시간 브로드캐스트
#     #    ✅ 최신 조합에서 broadcast=True/namespace 인자로 TypeError가 나는 경우가 있어 제거
#     #    기본 emit은 전체 클라이언트로 브로드캐스트됨.
#     try:
#         socketio.emit("new_message", msg)
#     except Exception:
#         # 브로드캐스트 실패가 HTTP 500로 번지지 않도록 보호
#         app.logger.exception("socketio emit failed")
#
#     # 5) HTTP 성공 응답 (생성됨 → 201)
#     return jsonify(ok=True, message=msg), 201
#
# # ──────────────────────────────────────────────────────────────────────
# # 웹소켓: 최초 연결 시 최근 20개 전달(옵션)
# # ──────────────────────────────────────────────────────────────────────
# @socketio.on("connect")
# def on_connect():
#     emit("init", {"messages": MESSAGES[-20:]})
#
# # ──────────────────────────────────────────────────────────────────────
# # 실행
# # ──────────────────────────────────────────────────────────────────────
# if __name__ == "__main__":
#     print("Run: http://127.0.0.1:5000  (Android emulator → http://10.0.2.2:5000)")
#     # 0.0.0.0 바인딩: 에뮬레이터/실기기에서 접속 가능. 방화벽 허용 필요.
#     socketio.run(app, host="0.0.0.0", port=5000, debug=True)

# server_secure.py
from flask import Flask, request, jsonify
from flask_cors import CORS
from werkzeug.security import generate_password_hash, check_password_hash
import time, secrets, jwt

app = Flask(__name__)
CORS(app)

# ================ 설정 ================
JWT_SECRET = "change_this_to_a_random_secret_in_prod"
JWT_ALG = "HS256"
TOKEN_TTL = 60 * 60 * 6   # 6 hours

# 데모용 인메모리 DB (실제 포트폴리오용이면 sqlite로 바꾸세요)
USERS = {}   # username -> {"pw_hash": "...", "created": ts}
MESSAGES = []  # {ts, user, text}

def now_ms(): return int(time.time() * 1000)

def make_jwt(username: str):
    payload = {"sub": username, "iat": int(time.time()), "exp": int(time.time()) + TOKEN_TTL}
    return jwt.encode(payload, JWT_SECRET, algorithm=JWT_ALG)

def verify_jwt(token: str):
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALG])
        return payload.get("sub")
    except Exception:
        return None

def auth_required(f):
    from functools import wraps
    @wraps(f)
    def wrapper(*args, **kwargs):
        auth = request.headers.get("Authorization", "")
        if not auth.startswith("Bearer "):
            return jsonify({"error": "missing token"}), 401
        user = verify_jwt(auth.split(" ",1)[1].strip())
        if not user:
            return jsonify({"error": "invalid/expired token"}), 401
        request.user = user
        return f(*args, **kwargs)
    return wrapper

# ================ 엔드포인트 ================
@app.get("/health")
def health():
    return {"status":"ok", "ts": now_ms()}

@app.post("/register")
def register():
    data = request.get_json(force=True)
    username = (data.get("username") or "").strip()
    password = (data.get("password") or "")
    if not username or not password:
        return jsonify({"error":"username/password required"}), 400
    if username in USERS:
        return jsonify({"error":"already exists"}), 409
    USERS[username] = {"pw_hash": generate_password_hash(password), "created": now_ms()}
    return {"ok": True}

@app.post("/login")
def login():
    data = request.get_json(force=True)
    username = (data.get("username") or "").strip()
    password = (data.get("password") or "")
    user = USERS.get(username)
    if not user or not check_password_hash(user["pw_hash"], password):
        return jsonify({"error":"invalid credentials"}), 401
    token = make_jwt(username)
    return {"token": token, "name": username, "expires_in": TOKEN_TTL}

@app.get("/messages")
@auth_required
def list_messages():
    limit = int(request.args.get("limit", 50))
    # 안전: 최신순 slice
    return {"messages": MESSAGES[-limit:]}

@app.post("/messages")
@auth_required
def post_message():
    data = request.get_json(force=True)
    text = (data.get("text") or "").strip()
    if not text:
        return jsonify({"error":"empty"}), 400
    msg = {"ts": now_ms(), "user": request.user, "text": text}
    MESSAGES.append(msg)
    return {"ok": True, "message": msg}, 201

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)

