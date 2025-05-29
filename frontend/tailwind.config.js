// tailwind.config.js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html", // Checks the root index.html
    "./src/**/*.{js,ts,jsx,tsx}", // Checks all relevant files in src
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'sans-serif'], // Ensure Inter font is linked in index.html or index.css
      },
      aspectRatio: {
        '1/1': '1 / 1',
      }
      // You can add more theme customizations here
    },
  },
  plugins: [],
}