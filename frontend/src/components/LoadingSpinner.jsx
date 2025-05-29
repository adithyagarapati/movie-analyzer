import React from 'react';

const LoadingSpinner = () => {
  return (
    <div className="flex justify-center items-center my-1">
      <div className="animate-spin rounded-full h-6 w-6 sm:h-8 sm:w-8 border-t-2 border-b-2 border-sky-500"></div>
    </div>
  );
};

export default LoadingSpinner;