import json
import time
import os

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
    Simple sentiment analysis using keyword matching (no external dependencies)
    """
    try:
        # Convert to lowercase for analysis
        text_lower = text.lower()
        
        # Define sentiment keywords
        positive_words = [
            'amazing', 'awesome', 'excellent', 'fantastic', 'great', 'wonderful', 
            'brilliant', 'outstanding', 'superb', 'magnificent', 'incredible',
            'love', 'perfect', 'beautiful', 'best', 'good', 'nice', 'enjoyed',
            'recommend', 'masterpiece', 'spectacular', 'phenomenal', 'delightful'
        ]
        
        negative_words = [
            'terrible', 'horrible', 'awful', 'bad', 'worst', 'hate', 'boring',
            'stupid', 'waste', 'disappointing', 'annoying', 'ridiculous',
            'pathetic', 'useless', 'disgusting', 'painful', 'dreadful',
            'appalling', 'abysmal', 'atrocious', 'mediocre', 'poor'
        ]
        
        # Count sentiment words
        positive_count = sum(1 for word in positive_words if word in text_lower)
        negative_count = sum(1 for word in negative_words if word in text_lower)
        
        # Calculate sentiment score
        total_words = len(text_lower.split())
        word_ratio = (positive_count - negative_count) / max(total_words, 1)
        
        # Normalize score to -1 to 1 range
        polarity = max(-1.0, min(1.0, word_ratio * 10))
        
        # Determine sentiment category
        if polarity > 0.1:
            sentiment = "positive"
        elif polarity < -0.1:
            sentiment = "negative"
        else:
            sentiment = "neutral"
        
        # Determine confidence level
        abs_polarity = abs(polarity)
        if abs_polarity > 0.3:
            confidence = "high"
        elif abs_polarity > 0.1:
            confidence = "medium"
        else:
            confidence = "low"
        
        # Generate rating based on sentiment (1-5 stars)
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