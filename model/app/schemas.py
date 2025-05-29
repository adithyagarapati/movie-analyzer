from pydantic import BaseModel
from typing import Union

class ReviewText(BaseModel):
    review_text: str

class PredictionOut(BaseModel):
    sentiment: str          # e.g., "Positive", "Negative", "Neutral"
    pseudo_rating: int      # e.g., 1-10
    model_info: Union[str, None] = None # Optional info about the model

class HealthCheck(BaseModel):
    status: str = "OK"

class DebugStatus(BaseModel):
    status: str