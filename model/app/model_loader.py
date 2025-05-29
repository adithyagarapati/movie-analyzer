# movie-review-model/app/model_loader.py (VADER Version)
import os
from pathlib import Path # Keep this if other parts of your project might use it
import nltk
from nltk.sentiment.vader import SentimentIntensityAnalyzer

model_metadata = "NLTK VADER Sentiment Analyzer v0.1 (Rule-Based)"
vader_analyzer = None

def load_model():
    """Initializes NLTK VADER."""
    global vader_analyzer
    try:
        # Check if the lexicon is already available
        nltk.data.find('sentiment/vader_lexicon.zip')
    except LookupError:
        print("NLTK VADER lexicon not found. Downloading...")
        try:
            nltk.download('vader_lexicon')
        except Exception as e:
            print(f"Failed to download vader_lexicon: {e}. Sentiment analysis might not work.")
            # Consider how to handle this - perhaps raise an error or set a flag
            return # Exit if download fails
    
    try:
        vader_analyzer = SentimentIntensityAnalyzer()
        print("NLTK VADER Sentiment Analyzer initialized successfully.")
    except Exception as e:
        print(f"Failed to initialize SentimentIntensityAnalyzer: {e}")


def predict_sentiment(text: str) -> tuple[str, int]:
    """
    Predicts sentiment using NLTK VADER and derives a pseudo-rating.
    """
    global vader_analyzer
    if vader_analyzer is None:
        print("WARN: VADER Analyzer not initialized. Returning default Neutral.")
        # Attempt to initialize it again if it failed earlier, or if load_model wasn't called
        # This is a fallback, ideally load_model succeeds at startup.
        load_model() 
        if vader_analyzer is None: # If still not initialized after retry
             return "Neutral", 5 

    try:
        scores = vader_analyzer.polarity_scores(text)
        compound = scores['compound']  # Compound score is from -1 (most negative) to +1 (most positive)

        sentiment_label = "Neutral"
        pseudo_rating = 5 # Default to neutral

        # Determine sentiment label and pseudo-rating based on compound score
        if compound >= 0.05:
            sentiment_label = "Positive"
            # Scale rating: compound 0.05 -> rating 6, compound 1.0 -> rating 10
            # Linear scaling: rating = m * compound + c
            # For (0.05, 6) and (1.0, 10):
            # 6 = m*0.05 + c
            # 10 = m*1.0 + c
            # Subtracting: 4 = m * 0.95  => m = 4 / 0.95 = 4.21
            # c = 6 - 4.21 * 0.05 = 6 - 0.2105 = 5.7895
            # rating = 4.21 * compound + 5.79 (approx)
            # Simpler: base 6, range of 4 points over compound range of 0.95
            pseudo_rating = 6 + round(((compound - 0.05) / (1.0 - 0.05)) * 4)

        elif compound <= -0.05:
            sentiment_label = "Negative"
            # Scale rating: compound -0.05 -> rating 4, compound -1.0 -> rating 1
            # Simpler: base 4, range of 3 points over compound range of 0.95 (from -0.05 to -1.0)
            # (compound is negative, so (compound - (-0.05)) will be more negative)
            # We want more negative compound to result in lower rating.
            # Range of compound for negative: -0.05 to -1.0 (span of 0.95)
            # Range of rating: 4 to 1 (span of 3)
            # Let's map [-1.0, -0.05] to [1, 4]
            # Normalize compound to [0, 1] within its negative range: (compound - (-1.0)) / (-0.05 - (-1.0))
            # = (compound + 1.0) / 0.95
            # Then scale to [0, 3] and add 1:  ((compound + 1.0) / 0.95) * 3 + 1
            normalized_neg_compound = (compound + 1.0) / 0.95 # This will be 0 for -1.0, and 1 for -0.05
            pseudo_rating = 1 + round(normalized_neg_compound * 3)

        else: # Neutral for compound scores between -0.05 and 0.05
            sentiment_label = "Neutral"
            # For -0.04 to 0.04, let's keep it around 5
            # A simple way: if compound is -0.04, rating 5. if 0, rating 5. if 0.04, rating 5.
            # Or slightly vary it: 0 -> 5, positive small -> 6, negative small -> 4
            if compound > 0: # e.g. 0.01 to 0.049
                pseudo_rating = 5 # Could be 6 for very slightly positive neutral
            elif compound < 0: # e.g. -0.01 to -0.049
                pseudo_rating = 5 # Could be 4 for very slightly negative neutral
            else: # compound is 0
                pseudo_rating = 5


        # Ensure rating is within 1-10 bounds as a final clamp
        pseudo_rating = max(1, min(10, pseudo_rating))

        print(f"VADER scores for '{text[:50]}...': compound={compound:.3f}, neu={scores['neu']:.3f}, neg={scores['neg']:.3f}, pos={scores['pos']:.3f} -> Sentiment: {sentiment_label}, Rating: {pseudo_rating}")
        return sentiment_label, pseudo_rating

    except Exception as e:
        print(f"Error during VADER sentiment prediction: {e}")
        return "Error", 0 # Indicate an error in prediction