from flask import Flask, request, jsonify
from flask_cors import CORS
import datetime

app = Flask(__name__)
CORS(app)  # Enable CORS so mobile devices and emulators can connect without restriction

@app.route("/", methods=["GET"])
def index():
    return jsonify({
        "status": "online",
        "service": "SurakshaX AI Inference Engine",
        "version": "1.0",
        "timestamp": datetime.datetime.utcnow().isoformat()
    }), 200

@app.route("/analyze_status", methods=["POST"])
def analyze_status():
    """
    Accepts telemetry JSON from SurakshaX Android App:
    {
        "audio_score": float (0.0 - 1.0, MFCC scream probability from ESC-50),
        "fall_force": float (1.0 - 10.0, SMV force from SisFall),
        "heart_rate": float (BPM, physiological stress from SWELL-KW),
        "latitude": float,
        "longitude": float
    }
    Returns:
    {
        "decision": "CRITICAL" | "SAFE",
        "reason": str,
        "timestamp": str
    }
    """
    try:
        data = request.get_json(force=True)
        audio = float(data.get("audio_score", 0.0))
        fall = float(data.get("fall_force", 0.0))
        hr = float(data.get("heart_rate", 70.0))
        lat = float(data.get("latitude", 0.0))
        lon = float(data.get("longitude", 0.0))

        # -------------------------------------------------------------
        # Hierarchical Decision Tree (HDT) calibrated on benchmark datasets:
        # 1. P1: Scream detected (Audio score >= 0.70 from ESC-50)
        # 2. P2: Sudden fall impact (Fall force >= 3.0 G from SisFall)
        # 3. P3: Severe distress (Heart rate >= 120 BPM from SWELL-KW)
        # -------------------------------------------------------------
        is_scream = audio >= 0.70
        is_fall = fall >= 3.0
        is_stress = hr >= 120.0 or hr <= 45.0

        if is_scream:
            decision = "CRITICAL"
            reason = "Acoustic distress / scream detected"
        elif is_fall and is_stress:
            decision = "CRITICAL"
            reason = "Severe physical fall combined with acute physiological stress"
        elif is_fall:
            decision = "CRITICAL"
            reason = "High-impact fall / physical trauma detected"
        elif is_stress and audio >= 0.50:
            decision = "CRITICAL"
            reason = "Elevated heart rate with alarming acoustic levels"
        else:
            decision = "SAFE"
            reason = "Vitals and environmental signals within normal thresholds"

        print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Telemetry: Audio={audio:.2f}, Fall={fall:.1f}G, HR={hr:.0f} BPM -> Decision: {decision} ({reason})")

        return jsonify({
            "decision": decision,
            "reason": reason,
            "location": {
                "latitude": lat,
                "longitude": lon
            },
            "metrics": {
                "audio_score": audio,
                "fall_force": fall,
                "heart_rate": hr
            },
            "timestamp": datetime.datetime.utcnow().isoformat()
        }), 200

    except Exception as e:
        print("Error processing inference request:", str(e))
        return jsonify({
            "error": "Invalid request payload",
            "details": str(e)
        }), 400

if __name__ == "__main__":
    # host='0.0.0.0' allows external devices on the same Wi-Fi (like your real phone) to connect
    print("=" * 60)
    print(" SurakshaX Local AI Inference Server Running on Port 5000")
    print(" - Android Emulator URL: http://10.0.2.2:5000/analyze_status")
    print(" - Localhost URL:        http://127.0.0.1:5000/analyze_status")
    print("=" * 60)
    app.run(host="0.0.0.0", port=5000, debug=True)
