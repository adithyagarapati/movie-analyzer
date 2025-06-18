import json
import time
import os

# Initialize NLTK data for TextBlob (do this once at module load)
try:
    import nltk
    import ssl
    
    # Handle SSL certificate issues in Lambda
    try:
        _create_unverified_https_context = ssl._create_unverified_context
    except AttributeError:
        pass
    else:
        ssl._create_default_https_context = _create_unverified_https_context
    
    # Download required NLTK data
    nltk.download('punkt', quiet=True)
    nltk.download('brown', quiet=True)
    print("📚 NLTK data initialized for TextBlob")
except Exception as e:
    print(f"⚠️ NLTK initialization warning: {e}")

def lambda_handler(event, context):
    """
    AWS Lambda handler for movie review sentiment analysis
    """
    
    try:
        # Parse the event
        if isinstance(event.get('body'), str):
            # API Gateway format
            body = json.loads(event['body'])
        else:
            # Direct Lambda invocation format
            body = event
        
        action = body.get('action', 'analyze')
        
        # Check if service is marked as unhealthy via environment variable
        service_healthy = os.getenv('LAMBDA_HEALTHY', 'true').lower() == 'true'
        
        if action == 'health':
            return create_response(
                200 if service_healthy else 503,
                {
                    "status": "healthy" if service_healthy else "unhealthy",
                    "service": "lambda-model",
                    "timestamp": time.time(),
                    "version": "1.0.0",
                    "message": "Lambda sentiment analysis service" + ("" if service_healthy else " (simulated failure)")
                }
            )
        
        elif action == 'status':
            return create_response(200, {
                "service": "lambda-model",
                "healthy": service_healthy,
                "version": "1.0.0",
                "timestamp": time.time(),
                "runtime": "python3.13",
                "capabilities": {
                    "sentiment_analysis": True,
                    "rating_generation": True,
                    "supported_sentiments": ["positive", "negative", "neutral"]
                },
                "statistics": {
                    "rating_ranges": {
                        "positive": "4.0-5.0 stars",
                        "neutral": "2.5-3.5 stars", 
                        "negative": "1.0-2.0 stars"
                    }
                }
            })
        
        elif action == 'analyze':
            # Check if service is healthy
            if not service_healthy:
                return create_response(503, {
                    "error": "Lambda service is unhealthy",
                    "message": "Sentiment analysis is temporarily unavailable"
                })
            
            # Validate input
            text = body.get('text', '').strip()
            
            if not text:
                return create_response(400, {
                    "error": "Text is required for analysis"
                })
            
            if not body.get('text'):
                return create_response(400, {
                    "error": "Text is required for analysis"
                })
            
            if len(text) > 5000:
                return create_response(400, {
                    "error": "Text too long",
                    "message": "Text must be less than 5000 characters"
                })
            
            # Perform sentiment analysis
            result = analyze_sentiment_and_rating(text)
            
            # Add metadata
            result.update({
                "timestamp": time.time(),
                "text_length": len(text),
                "processed_by": "lambda-textblob"
            })
            
            print(f"📊 Lambda analyzed: '{text[:50]}...' → {result['sentiment']} ({result['score']}) → {result['rating']} stars")
            
            return create_response(200, result)
        
        else:
            return create_response(400, {
                "error": "Unknown action: " + str(action),
                "valid_actions": ["analyze", "health", "status"]
            })
    
    except Exception as e:
        print(f"❌ Lambda error: {str(e)}")
        return create_response(500, {
            "error": "Analysis failed",
            "message": str(e)
        })

def analyze_sentiment_and_rating(text):
    """
    Analyze sentiment and generate rating using TextBlob (same as original model)
    """
    try:
        # Use TextBlob for sentiment analysis (same as original model)
        from textblob import TextBlob
        
        blob = TextBlob(text)
        polarity = blob.sentiment.polarity  # Range: -1 (negative) to 1 (positive)
        
        # Determine sentiment category
        if polarity > 0.1:
            sentiment = "positive"
        elif polarity < -0.1:
            sentiment = "negative"
        else:
            sentiment = "neutral"
        
        # Determine confidence level
        abs_polarity = abs(polarity)
        if abs_polarity > 0.5:
            confidence = "high"
        elif abs_polarity > 0.2:
            confidence = "medium"
        else:
            confidence = "low"
        
        # Generate rating based on sentiment (1-5 stars) - same logic as original
        if sentiment == "positive":
            # Positive: 4-5 stars, higher polarity = higher rating
            base_rating = 4.0 + (polarity * 1.0)  # 4.0 to 5.0
            rating = round(min(5.0, max(4.0, base_rating)), 1)
        elif sentiment == "negative":
            # Negative: 1-2 stars, more negative = lower rating
            base_rating = 2.0 + (polarity * 1.0)  # 1.0 to 2.0 (since polarity is negative)
            rating = round(max(1.0, min(2.0, base_rating)), 1)
        else:
            # Neutral: around 3 stars with slight variation
            base_rating = 3.0 + (polarity * 0.5)  # 2.5 to 3.5
            rating = round(max(2.5, min(3.5, base_rating)), 1)
        
        return {
            "sentiment": sentiment,
            "score": round(polarity, 3),
            "confidence": confidence,
            "rating": rating
        }
    
    except Exception as e:
        # Fallback to neutral if analysis fails
        return {
            "sentiment": "neutral",
            "score": 0.0,
            "confidence": "low",
            "rating": 3.0,
            "error": str(e)
        }

def create_response(status_code, body):
    """
    Create properly formatted Lambda response
    """
    return {
        'statusCode': status_code,
        'headers': {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type'
        },
        'body': json.dumps(body)
    } 