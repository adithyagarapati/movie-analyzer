# S3 Image Configuration Guide

This guide explains how to easily switch from local images to S3 images for the Movie Analyzer frontend.

## Quick Setup Options

### Option 1: Update URLs in Configuration File (Recommended)

1. Open `src/config/imageConfig.js`
2. Replace the placeholder URLs in `S3_IMAGE_URLS` with your actual S3 URLs:

```javascript
const S3_IMAGE_URLS = {
  'shawshank': 'https://your-actual-bucket.s3.amazonaws.com/movie-images/shawshank-redemption.jpg',
  'inception': 'https://your-actual-bucket.s3.amazonaws.com/movie-images/inception.jpg',
  'interstellar': 'https://your-actual-bucket.s3.amazonaws.com/movie-images/interstellar.jpg',
  'fight-club': 'https://your-actual-bucket.s3.amazonaws.com/movie-images/fight-club.jpg',
  'gladiator': 'https://your-actual-bucket.s3.amazonaws.com/movie-images/gladiator.jpg',
  'dark-knight': 'https://your-actual-bucket.s3.amazonaws.com/movie-images/dark-knight.jpg',
};
```

3. Make sure `source: 's3'` is set in the IMAGE_CONFIG object
4. Restart your application

### Option 2: Use the Browser Console (For Quick Testing)

Open the browser console and use these helper functions:

```javascript
// For a standard S3 bucket setup:
import { setupS3Bucket } from './src/utils/imageUtils.js';
setupS3Bucket('your-bucket-name', 'us-east-1', 'movie-images');

// For custom URLs:
import { setCustomS3Urls } from './src/utils/imageUtils.js';
setCustomS3Urls({
  'shawshank': 'https://your-cdn.com/shawshank.jpg',
  'inception': 'https://your-cdn.com/inception.jpg',
  // ... other movies
});
```

### Option 3: Programmatic Setup in Code

Add this to your `App.js` or any component:

```javascript
import { setupS3Bucket, setCustomS3Urls } from './utils/imageUtils';

// In your component or useEffect:
useEffect(() => {
  // Method 1: Standard S3 bucket
  setupS3Bucket('my-movie-bucket', 'us-west-2', 'images/movies');
  
  // Method 2: Custom URLs
  setCustomS3Urls({
    'shawshank': 'https://mybucket.s3.amazonaws.com/shawshank-redemption.jpg',
    'inception': 'https://mybucket.s3.amazonaws.com/inception.jpg',
    'interstellar': 'https://mybucket.s3.amazonaws.com/interstellar.jpg',
    'fight-club': 'https://mybucket.s3.amazonaws.com/fight-club.jpg',
    'gladiator': 'https://mybucket.s3.amazonaws.com/gladiator.jpg',
    'dark-knight': 'https://mybucket.s3.amazonaws.com/dark-knight.jpg',
  });
}, []);
```

## Expected File Names in S3

Upload your images to S3 with these exact file names:
- `shawshank-redemption.jpg`
- `inception.jpg`
- `interstellar.jpg`
- `fight-club.jpg`
- `gladiator.jpg`
- `dark-knight.jpg`

## Switching Between Local and S3

### Switch to S3 Images:
```javascript
import { useS3Images } from './utils/imageUtils';
useS3Images();
```

### Switch Back to Local Images:
```javascript
import { useLocalImages } from './utils/imageUtils';
useLocalImages();
```

## Testing Your Setup

1. After configuring S3 URLs, restart your application
2. Check the browser console for confirmation messages
3. Verify images load correctly in the movie grid
4. If images don't load, check:
   - S3 bucket permissions (must allow public read)
   - CORS configuration on your S3 bucket
   - Image file names match exactly

## S3 Bucket Configuration

Make sure your S3 bucket has:

### Public Read Access
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::your-bucket-name/*"
    }
  ]
}
```

### CORS Configuration
```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET"],
    "AllowedOrigins": ["*"],
    "ExposeHeaders": []
  }
]
```

## Adding New Movies

To add a new movie with S3 images:

1. Add the movie to `src/data/movies.js`
2. Add the S3 URL mapping in `src/config/imageConfig.js`
3. The system will automatically use the configured image source

## Troubleshooting

- **Images not loading**: Check browser console for errors
- **CORS errors**: Verify S3 CORS configuration
- **404 errors**: Verify file names and S3 URLs are correct
- **Fallback**: System automatically falls back to local images if S3 URLs fail 