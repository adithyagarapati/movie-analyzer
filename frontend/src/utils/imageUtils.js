// Image utilities for easy S3 URL management

import { setMovieS3Url, getAllS3Urls, setImageSource } from '../config/imageConfig';

/**
 * Batch update S3 URLs for multiple movies
 * @param {object} urlMappings - Object with movieId: s3Url pairs
 * 
 * Example usage:
 * updateMultipleS3Urls({
 *   'shawshank': 'https://mybucket.s3.amazonaws.com/movies/shawshank.jpg',
 *   'inception': 'https://mybucket.s3.amazonaws.com/movies/inception.jpg'
 * });
 */
export const updateMultipleS3Urls = (urlMappings) => {
  Object.entries(urlMappings).forEach(([movieId, s3Url]) => {
    setMovieS3Url(movieId, s3Url);
  });
  console.log('Updated S3 URLs for movies:', Object.keys(urlMappings));
};

/**
 * Quick setup for a complete S3 bucket
 * @param {string} bucketName - Your S3 bucket name
 * @param {string} region - AWS region (optional, defaults to us-east-1)
 * @param {string} path - Path within bucket (optional)
 * 
 * Example usage:
 * setupS3Bucket('my-movie-bucket', 'us-west-2', 'images/movies');
 */
export const setupS3Bucket = (bucketName, region = 'us-east-1', path = '') => {
  const baseUrl = `https://${bucketName}.s3.amazonaws.com`;
  const fullPath = path ? `${path}/` : '';
  
  const urlMappings = {
    'shawshank': `${baseUrl}/${fullPath}shawshank-redemption.jpg`,
    'inception': `${baseUrl}/${fullPath}inception.jpg`,
    'interstellar': `${baseUrl}/${fullPath}interstellar.jpg`,
    'fight-club': `${baseUrl}/${fullPath}fight-club.jpg`,
    'gladiator': `${baseUrl}/${fullPath}gladiator.jpg`,
    'dark-knight': `${baseUrl}/${fullPath}dark-knight.jpg`,
  };
  
  updateMultipleS3Urls(urlMappings);
  setImageSource('s3');
  
  console.log(`✅ S3 setup complete for bucket: ${bucketName}`);
  console.log('Updated URLs:', urlMappings);
  
  return urlMappings;
};

/**
 * Switch back to local images
 */
export const useLocalImages = () => {
  setImageSource('local');
  console.log('✅ Switched to local images');
};

/**
 * Switch to S3 images
 */
export const useS3Images = () => {
  setImageSource('s3');
  console.log('✅ Switched to S3 images');
};

/**
 * Get current configuration status
 */
export const getImageConfig = () => {
  return {
    currentSource: localStorage.getItem('imageSource') || 's3',
    s3Urls: getAllS3Urls()
  };
};

/**
 * Easy setup function - just provide your S3 URLs
 * @param {object} customUrls - Custom S3 URLs for each movie
 * 
 * Example usage:
 * setCustomS3Urls({
 *   'shawshank': 'https://cdn.example.com/shawshank.jpg',
 *   'inception': 'https://cdn.example.com/inception.jpg'
 * });
 */
export const setCustomS3Urls = (customUrls) => {
  updateMultipleS3Urls(customUrls);
  setImageSource('s3');
  
  console.log('✅ Custom S3 URLs set successfully');
  return customUrls;
}; 