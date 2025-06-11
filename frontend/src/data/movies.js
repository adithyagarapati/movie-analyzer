// Fixed list of 6 movies as specified in rules
// Now uses configurable image URLs (local or S3)
// Updated IDs to match database values for preloaded reviews

import { getMovieImageUrl } from '../config/imageConfig';

export const MOVIES = [
  {
    id: 'shawshank',
    title: 'The Shawshank Redemption',
    get thumbnail() {
      return getMovieImageUrl('shawshank');
    },
    year: 1994,
    genre: 'Drama'
  },
  {
    id: 'inception',
    title: 'Inception',
    get thumbnail() {
      return getMovieImageUrl('inception');
    },
    year: 2010,
    genre: 'Sci-Fi'
  },
  {
    id: 'interstellar',
    title: 'Interstellar',
    get thumbnail() {
      return getMovieImageUrl('interstellar');
    },
    year: 2014,
    genre: 'Sci-Fi'
  },
  {
    id: 'fight-club',
    title: 'Fight Club',
    get thumbnail() {
      return getMovieImageUrl('fight-club');
    },
    year: 1999,
    genre: 'Drama'
  },
  {
    id: 'gladiator',
    title: 'Gladiator',
    get thumbnail() {
      return getMovieImageUrl('gladiator');
    },
    year: 2000,
    genre: 'Action'
  },
  {
    id: 'dark-knight',
    title: 'Dark Knight',
    get thumbnail() {
      return getMovieImageUrl('dark-knight');
    },
    year: 2008,
    genre: 'Action'
  }
]; 