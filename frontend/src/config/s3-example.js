// EXAMPLE: How to set up S3 URLs
// Copy the relevant code below to your imageConfig.js file

// STEP 1: Replace the S3_IMAGE_URLS object in imageConfig.js with your actual URLs:

const S3_IMAGE_URLS = {
  // Replace these with your actual S3 URLs:
  'shawshank': 'https://your-bucket-name.s3.amazonaws.com/movie-images/shawshank-redemption.jpg',
  'inception': 'https://your-bucket-name.s3.amazonaws.com/movie-images/inception.jpg',
  'interstellar': 'https://your-bucket-name.s3.amazonaws.com/movie-images/interstellar.jpg',
  'fight-club': 'https://your-bucket-name.s3.amazonaws.com/movie-images/fight-club.jpg',
  'gladiator': 'https://your-bucket-name.s3.amazonaws.com/movie-images/gladiator.jpg',
  'dark-knight': 'https://your-bucket-name.s3.amazonaws.com/movie-images/dark-knight.jpg',
};

// STEP 2: Change the source to 's3' in the IMAGE_CONFIG:

const IMAGE_CONFIG = {
  source: 's3', // Changed from 'local' to 's3'
  // ... rest of configuration
};

// STEP 3: Alternative - Use the utility functions:

// Option A: If all your images follow the same pattern in one bucket:
import { setupS3Bucket } from '../utils/imageUtils';
setupS3Bucket('your-bucket-name', 'us-east-1', 'movie-images');

// Option B: If you have custom URLs for each image:
import { setCustomS3Urls } from '../utils/imageUtils';
setCustomS3Urls({
  'shawshank': 'https://example-cdn.com/movies/shawshank.jpg',
  'inception': 'https://example-cdn.com/movies/inception.jpg',
  'interstellar': 'https://example-cdn.com/movies/interstellar.jpg',
  'fight-club': 'https://example-cdn.com/movies/fight-club.jpg',
  'gladiator': 'https://example-cdn.com/movies/gladiator.jpg',
  'dark-knight': 'https://example-cdn.com/movies/dark-knight.jpg',
});

// STEP 4: Test in browser console (open browser dev tools and paste this):

import { getImageConfig } from './src/utils/imageUtils.js';
console.log('Current image configuration:', getImageConfig());

// That's it! Your app will now use S3 images instead of local ones.

/* 
UPLOAD CHECKLIST for S3:
1. Upload these files to your S3 bucket:
   - shawshank-redemption.jpg
   - inception.jpg
   - interstellar.jpg
   - fight-club.jpg
   - gladiator.jpg
   - dark-knight.jpg

2. Make sure your S3 bucket allows public read access
3. Configure CORS if needed (see S3_IMAGE_SETUP.md)
4. Update the URLs above with your actual bucket name and region
*/ 