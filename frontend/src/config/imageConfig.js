// Image Configuration
// Switch between local and S3 images easily

// Configuration settings
const IMAGE_CONFIG = {
  // Set to 'local' to use local images, 's3' to use S3 URLs
  source: 's3', // Change this to 's3' when you have S3 URLs configured
  
  // S3 configuration
  s3: {
    baseUrl: 'https://your-bucket-name.s3.amazonaws.com', // Replace with your actual S3 bucket URL
    path: 'movie-images', // Path within the bucket (optional)
  },
  
  // Local configuration
  local: {
    baseUrl: '',
    path: '/images/movies',
  }
};

// Movie image mappings - Easy to update S3 URLs here
const S3_IMAGE_URLS = {
  'shawshank': 'https://your-bucket-name.s3.amazonaws.com/movie-images/shawshank-redemption.jpg',
  'inception': 'https://your-bucket-name.s3.amazonaws.com/movie-images/inception.jpg',
  'interstellar': 'https://your-bucket-name.s3.amazonaws.com/movie-images/interstellar.jpg',
  'fight-club': 'https://your-bucket-name.s3.amazonaws.com/movie-images/fight-club.jpg',
  'gladiator': 'https://your-bucket-name.s3.amazonaws.com/movie-images/gladiator.jpg',
  'dark-knight': 'https://your-bucket-name.s3.amazonaws.com/movie-images/dark-knight.jpg',
};

// Local image mappings
const LOCAL_IMAGE_URLS = {
  'shawshank': '/images/movies/shawshank-redemption.jpg',
  'inception': '/images/movies/inception.jpg',
  'interstellar': '/images/movies/interstellar.jpg',
  'fight-club': '/images/movies/fight-club.jpg',
  'gladiator': '/images/movies/gladiator.jpg',
  'dark-knight': '/images/movies/dark-knight.jpg',
};

/**
 * Get the image URL for a movie
 * @param {string} movieId - The movie ID
 * @returns {string} - The complete image URL
 */
export const getMovieImageUrl = (movieId) => {
  if (IMAGE_CONFIG.source === 's3') {
    return S3_IMAGE_URLS[movieId] || LOCAL_IMAGE_URLS[movieId]; // Fallback to local if S3 URL not found
  } else {
    return LOCAL_IMAGE_URLS[movieId];
  }
};

/**
 * Add or update an S3 URL for a movie
 * @param {string} movieId - The movie ID
 * @param {string} s3Url - The S3 URL
 */
export const setMovieS3Url = (movieId, s3Url) => {
  S3_IMAGE_URLS[movieId] = s3Url;
};

/**
 * Get all current S3 URLs
 * @returns {object} - Object with movieId: s3Url mappings
 */
export const getAllS3Urls = () => {
  return { ...S3_IMAGE_URLS };
};

/**
 * Switch image source
 * @param {string} source - 'local' or 's3'
 */
export const setImageSource = (source) => {
  IMAGE_CONFIG.source = source;
};

export default IMAGE_CONFIG; 