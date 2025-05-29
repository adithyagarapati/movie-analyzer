from fastapi import FastAPI, HTTPException, Body
from .schemas import ReviewText, PredictionOut, HealthCheck, DebugStatus
from .model_loader import load_model, predict_sentiment, model_metadata
import os

# --- Application State for Debug/Demo ---
model_service_operational = True

# --- FastAPI App Initialization ---
app = FastAPI(
    title="Movie Review Sentiment Model API",
    description="API to predict sentiment and pseudo-rating from movie review text.",
    version="1.0.0"
)

@app.on_event("startup")
async def startup_event():
    print("Model service starting up...")
    load_model() # Load the model when the application starts
    print(f"Model service operational state: {model_service_operational}")

# --- Debug Endpoints ---
@app.post("/debug/model/enable", response_model=DebugStatus, tags=["Debug"])
async def enable_model_service():
    """Enables the model service for predictions."""
    global model_service_operational
    model_service_operational = True
    print("DEBUG: Model service ENABLED via API.")
    return DebugStatus(status="Model service enabled")

@app.post("/debug/model/disable", response_model=DebugStatus, tags=["Debug"])
async def disable_model_service():
    """Disables the model service; /predict will return an error."""
    global model_service_operational
    model_service_operational = False
    print("DEBUG: Model service DISABLED via API.")
    return DebugStatus(status="Model service disabled")

# --- Core Prediction Endpoint ---
@app.post("/predict", response_model=PredictionOut, tags=["Prediction"])
async def get_prediction(review: ReviewText = Body(...)):
    """
    Predicts sentiment and a pseudo-rating from review text.
    """
    global model_service_operational
    if not model_service_operational:
        print("Attempted to call /predict while model service is DISABLED.")
        raise HTTPException(status_code=503, detail="Model service is temporarily disabled for demo.")

    try:
        sentiment, rating = predict_sentiment(review.review_text)
        if sentiment == "Error": # If prediction itself had an internal error
             raise HTTPException(status_code=500, detail="Error during sentiment prediction process.")
        return PredictionOut(sentiment=sentiment, pseudo_rating=rating, model_info=model_metadata)
    except Exception as e:
        print(f"Unexpected error in /predict endpoint: {e}")
        raise HTTPException(status_code=500, detail=f"An unexpected error occurred: {str(e)}")

# --- Health Check Endpoints for Kubernetes ---
@app.get("/livez", response_model=HealthCheck, tags=["Health"])
async def liveness_probe():
    """Liveness probe: Checks if the application is running."""
    return HealthCheck(status="OK")

@app.get("/readyz", response_model=HealthCheck, tags=["Health"])
async def readiness_probe():
    """
    Readiness probe: Checks if the application is ready to serve requests.
    For the model service, this means the model is loaded and the service is operational.
    """
    global model_service_operational
    from .model_loader import model # Access the loaded model instance

    if not model_service_operational:
        print("READINESS PROBE: Model service NOT READY (disabled via API).")
        raise HTTPException(status_code=503, detail="Model service disabled via API.")
    if model is None and MODEL_PATH.exists(): # Model file exists but failed to load
        print("READINESS PROBE: Model service NOT READY (model failed to load).")
        raise HTTPException(status_code=503, detail="Model failed to load.")
    # If model.pkl is optional or has a fallback, this check might need adjustment.
    # For now, if no model_path implies dummy model, it's considered "ready" if operational.

    print("READINESS PROBE: Model service READY.")
    return HealthCheck(status="OK")

if __name__ == "__main__":
    import uvicorn
    # This is for local development. Docker will use the CMD in Dockerfile.
    # Get port from environment variable or default to 8000
    port = int(os.getenv("PORT", "8000"))
    uvicorn.run(app, host="0.0.0.0", port=port)