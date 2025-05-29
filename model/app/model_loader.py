import joblib
import os
from pathlib import Path

# --- For a real scikit-learn model ---
MODEL_DIR = Path(__file__).resolve().parent.parent / "model" # Points to the 'model' directory
MODEL_PATH = MODEL_DIR / "model.pkl"

# --- Placeholder for NLTK VADER if you prefer a rule-based approach (no .pkl needed) ---
# import nltk
# from nltk.sentiment.vader import SentimentIntensityAnalyzer
# try:
#     nltk.data.find('sentiment/vader_lexicon.zip')
# except nltk.downloader.DownloadError:
#     nltk.download('vader_lexicon')
# vader_analyzer = SentimentIntensityAnalyzer()
# --- End NLTK VADER Placeholder ---


model = None
model_metadata = "Scikit-learn Sentiment Model v1.0" # Example metadata

def load_model():
    """Loads the sentiment model from disk."""
    global model
    if not MODEL_PATH.exists():
        # Fallback or error if model.pkl is missing
        print(f"WARNING: Model file not found at {MODEL_PATH}. Predictions will be dummied.")
        # You could load a dummy model here or raise an error
        # For now, let's allow it to proceed with model as None, and predict_sentiment will handle it.
        return

    try:
        model = joblib.load(MODEL_PATH)
        print(f"Sentiment model loaded successfully from {MODEL_PATH}")
    except Exception as e:
        print(f"Error loading model from {MODEL_PATH}: {e}")
        # Potentially raise an error or use a dummy model
        model = None


def predict_sentiment(text: str) -> tuple[str, int]:
    """
    Predicts sentiment and derives a pseudo-rating.
    Adjust this function based on your actual model's input/output.
    """
    global model

    if model is None: # If model failed to load or not found
        print("WARN: Model not loaded. Returning dummy prediction.")
        # Dummy prediction logic
        if "bad" in text.lower() or "terrible" in text.lower():
            sentiment_label = "Negative"
            pseudo_rating = 2
        elif "great" in text.lower() or "excellent" in text.lower() or "awesome" in text.lower():
            sentiment_label = "Positive"
            pseudo_rating = 9
        else:
            sentiment_label = "Neutral"
            pseudo_rating = 5
        return sentiment_label, pseudo_rating

    # --- Actual prediction logic for a scikit-learn model ---
    # This assumes your model's predict() method returns a label or class index.
    # You might need to preprocess 'text' if your pipeline doesn't do it.
    try:
        # Example: if model.predict() returns ["Positive"], ["Negative"], etc.
        prediction = model.predict([text])[0] # Pass text as a list for many sklearn vectorizers

        # Map prediction to sentiment label and pseudo-rating
        # This mapping depends on what your model actually outputs
        if isinstance(prediction, (int, float)): # If model outputs class indices (0, 1, 2)
            if prediction == 0: # Assuming 0 is Negative
                sentiment_label = "Negative"
                pseudo_rating = 2
            elif prediction == 2: # Assuming 2 is Positive
                sentiment_label = "Positive"
                pseudo_rating = 9
            else: # Assuming 1 is Neutral
                sentiment_label = "Neutral"
                pseudo_rating = 5
        elif isinstance(prediction, str): # If model directly outputs "Positive", "Negative"
            sentiment_label = prediction
            if sentiment_label == "Positive":
                pseudo_rating = 9
            elif sentiment_label == "Negative":
                pseudo_rating = 2
            else:
                pseudo_rating = 5
        else:
            print(f"WARN: Unknown model prediction format: {prediction}. Defaulting to Neutral.")
            sentiment_label = "Neutral"
            pseudo_rating = 5

        return sentiment_label, pseudo_rating

    except Exception as e:
        print(f"Error during model prediction: {e}")
        # Fallback in case of prediction error
        return "Error", 0


    # --- If using NLTK VADER (Placeholder) ---
    # scores = vader_analyzer.polarity_scores(text)
    # compound = scores['compound']
    # if compound >= 0.05:
    #     sentiment_label = "Positive"
    #     pseudo_rating = int(max(7, min(10, 7 + compound * 3))) # Scale 7-10
    # elif compound <= -0.05:
    #     sentiment_label = "Negative"
    #     pseudo_rating = int(max(1, min(4, 4 + compound * 3)))  # Scale 1-4
    # else:
    #     sentiment_label = "Neutral"
    #     pseudo_rating = int(max(4, min(7, 5 + compound * 2))) # Scale 4-7
    # return sentiment_label, pseudo_rating
    # --- End NLTK VADER ---