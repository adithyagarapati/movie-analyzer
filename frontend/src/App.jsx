// src/App.jsx
import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { movies } from './movies';
import { Toaster, toast } from 'react-hot-toast';
import { motion, AnimatePresence } from 'framer-motion';
import { loadAppConfig, getBackendApiUrl } from './apiConfig';
import AnimatedSection from './components/AnimatedSection'; // Keep this for consistency
import LoadingSpinner from './components/LoadingSpinner';   // Keep this

// --- Icon examples (install react-icons: npm install react-icons) ---
import { FiFilm, FiMessageSquare, FiBarChart2, FiAlertTriangle, FiCheckCircle, FiXCircle, FiClock } from 'react-icons/fi';

function App() {
    const [selectedMovie, setSelectedMovie] = useState(movies[0].name);
    const [reviewText, setReviewText] = useState('');
    const [analysisResult, setAnalysisResult] = useState(null);
    const [isLoadingAnalysis, setIsLoadingAnalysis] = useState(false);
    const [isLoadingHistory, setIsLoadingHistory] = useState(false);
    const [movieHistory, setMovieHistory] = useState([]);
    const [backendError, setBackendError] = useState(null);
    const [isAppConfigLoaded, setIsAppConfigLoaded] = useState(false);

    const selectedMovieData = movies.find(m => m.name === selectedMovie);

    // --- Initialize App Config ---
    useEffect(() => {
        async function initializeApp() {
            try {
                await loadAppConfig();
                setIsAppConfigLoaded(true);
            } catch (error) {
                setIsAppConfigLoaded(true);
                toast.error("Critical error: Could not load application configuration.");
            }
        }
        initializeApp();
    }, []);

    // --- Fetch Movie History ---
    const fetchMovieHistory = useCallback(async (movieName) => {
        if (!isAppConfigLoaded) return;
        setIsLoadingHistory(true);
        setBackendError(null);
        const BACKEND_URL = getBackendApiUrl();
        try {
            const response = await axios.get(`${BACKEND_URL}/reviews_history/${encodeURIComponent(movieName)}`);
            setMovieHistory(response.data || []);
        } catch (error) {
            let errorMessage = 'Failed to fetch review history.';
            if (error.response) errorMessage = `Error: ${error.response.data?.message || error.response.statusText || `Backend responded with ${error.response.status}`}`;
            else if (error.request) errorMessage = 'Backend not responding for history. Please try again later.';
            setBackendError(errorMessage); toast.error(errorMessage); setMovieHistory([]);
        } finally { setIsLoadingHistory(false); }
    }, [isAppConfigLoaded]);

    useEffect(() => {
        if (selectedMovie && isAppConfigLoaded) {
            fetchMovieHistory(selectedMovie);
            setAnalysisResult(null); setBackendError(null);
        }
    }, [selectedMovie, fetchMovieHistory, isAppConfigLoaded]);

    // --- Handle Analyze Review ---
    const handleAnalyzeReview = async () => {
        if (!isAppConfigLoaded) { toast.error("Application is not ready."); return; }
        if (!reviewText.trim()) { toast.error('Please write a review!'); return; }
        setIsLoadingAnalysis(true); setAnalysisResult(null); setBackendError(null);
        const BACKEND_URL = getBackendApiUrl();
        try {
            const response = await axios.post(`${BACKEND_URL}/analyze_review`, {
                movie_name: selectedMovie, review_text: reviewText,
            });
            setAnalysisResult(response.data);
            toast.custom((t) => (
                <motion.div
                    initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 20 }}
                    className={`${t.visible ? 'animate-enter' : 'animate-leave'} max-w-md w-full bg-green-600 shadow-lg rounded-lg pointer-events-auto flex ring-1 ring-black ring-opacity-5`}
                >
                    <div className="flex-1 w-0 p-4">
                        <div className="flex items-start">
                            <div className="flex-shrink-0 pt-0.5 text-white">
                                <FiCheckCircle className="h-6 w-6" />
                            </div>
                            <div className="ml-3 flex-1">
                                <p className="text-sm font-medium text-white">Success!</p>
                                <p className="mt-1 text-sm text-green-100">Review analyzed successfully!</p>
                            </div>
                        </div>
                    </div>
                    <div className="flex border-l border-green-500">
                        <button onClick={() => toast.dismiss(t.id)} className="w-full border border-transparent rounded-none rounded-r-lg p-4 flex items-center justify-center text-sm font-medium text-white hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500">
                            Close
                        </button>
                    </div>
                </motion.div>
            ));
            fetchMovieHistory(selectedMovie); setReviewText('');
        } catch (error) {
            let errorMessage = 'Failed to analyze review.';
            if (error.response) errorMessage = `Error: ${error.response.data?.message || error.response.statusText || `Backend responded with ${error.response.status}`}`;
            else if (error.request) errorMessage = 'Backend not responding for analysis. Please try again later.';
            setBackendError(errorMessage);
            toast.custom((t) => (
                 <motion.div
                    initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 20 }}
                    className={`${t.visible ? 'animate-enter' : 'animate-leave'} max-w-md w-full bg-red-600 shadow-lg rounded-lg pointer-events-auto flex ring-1 ring-black ring-opacity-5`} >
                    <div className="flex-1 w-0 p-4">
                        <div className="flex items-start">
                            <div className="flex-shrink-0 pt-0.5 text-white"> <FiXCircle className="h-6 w-6" /> </div>
                            <div className="ml-3 flex-1"> <p className="text-sm font-medium text-white">Error!</p> <p className="mt-1 text-sm text-red-100">{errorMessage}</p> </div>
                        </div>
                    </div>
                    <div className="flex border-l border-red-500"> <button onClick={() => toast.dismiss(t.id)} className="w-full border border-transparent rounded-none rounded-r-lg p-4 flex items-center justify-center text-sm font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500">Close</button> </div>
                </motion.div>
            ));
            setAnalysisResult(null);
        } finally { setIsLoadingAnalysis(false); }
    };

    // --- Initial Loading Screen ---
    if (!isAppConfigLoaded && !backendError) {
        return (
            <div className="min-h-screen bg-slate-900 text-white flex flex-col justify-center items-center p-4">
                <LoadingSpinner />
                <motion.p initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.5 }} className="mt-6 text-xl font-medium text-slate-300">
                    Initializing Analyzer...
                </motion.p>
            </div>
        );
    }

    // --- Main UI ---
    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-slate-100 p-4 sm:p-8 font-sans antialiased">
            <Toaster position="top-center" />
            <motion.header
                initial={{ opacity: 0, y: -60 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, type: 'spring', stiffness: 100 }}
                className="text-center mb-12 sm:mb-16"
            >
                <h1 className="text-5xl sm:text-6xl lg:text-7xl font-bold tracking-tight">
                    <span className="text-transparent bg-clip-text bg-gradient-to-r from-sky-400 via-pink-400 to-purple-500">Movie</span>
                    <span className="text-transparent bg-clip-text bg-gradient-to-r from-purple-500 via-teal-400 to-sky-400 ml-2">Review</span>
                    <span className="text-slate-300 ml-2">Analyzer</span>
                </h1>
                <p className="mt-4 text-lg text-slate-400 max-w-2xl mx-auto">
                    Select a movie, share your thoughts, and let our AI tell you the sentiment!
                </p>
            </motion.header>

            <div className="container mx-auto max-w-7xl grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-start">
                {/* Left Column */}
                <motion.section
                    initial={{ opacity: 0, x: -50 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.6, delay: 0.2, ease: "easeOut" }}
                    className="lg:col-span-4 bg-slate-800/60 backdrop-blur-xl p-6 sm:p-8 rounded-2xl shadow-2xl border border-slate-700/50"
                >
                    <div className="flex items-center mb-6">
                        <FiFilm className="h-8 w-8 text-sky-400 mr-3" />
                        <h2 className="text-3xl font-semibold text-sky-300">Select Movie</h2>
                    </div>
                    <div className="mb-8 relative group">
                        {selectedMovieData && (
                            <motion.div layoutId={`movie-image-${selectedMovieData.name}`} className="rounded-lg overflow-hidden shadow-xl mb-4 aspect-square transition-all duration-300 group-hover:scale-105">
                                <img src={selectedMovieData.image} alt={selectedMovieData.name} className="w-full h-full object-cover " />
                            </motion.div>
                        )}
                        <select
                            value={selectedMovie} onChange={(e) => setSelectedMovie(e.target.value)}
                            className="w-full p-4 bg-slate-700/80 border-2 border-slate-600 rounded-lg text-slate-100 text-lg focus:ring-2 focus:ring-sky-500 focus:border-sky-500 outline-none transition-all appearance-none custom-select"
                        >
                            {movies.map((movie) => (<option key={movie.name} value={movie.name}>{movie.name}</option>))}
                        </select>
                         <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 text-slate-400">
                            <svg className="fill-current h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z"/></svg>
                        </div>
                    </div>

                    <div className="flex items-center mb-4">
                        <FiMessageSquare className="h-7 w-7 text-sky-400 mr-3" />
                        <h2 className="text-2xl font-semibold text-sky-300">Your Review</h2>
                    </div>
                    <textarea
                        value={reviewText} onChange={(e) => setReviewText(e.target.value)} rows="6"
                        placeholder={`What did you think of ${selectedMovie}? Be honest!`}
                        className="w-full p-4 bg-slate-700/80 border-2 border-slate-600 rounded-lg text-slate-200 placeholder-slate-500 focus:ring-2 focus:ring-sky-500 focus:border-sky-500 outline-none resize-none transition-all text-md"
                    />
                    <motion.button
                        onClick={handleAnalyzeReview} disabled={isLoadingAnalysis || !isAppConfigLoaded}
                        className="mt-6 w-full bg-gradient-to-r from-sky-500 via-blue-500 to-purple-600 hover:from-sky-600 hover:via-blue-600 hover:to-purple-700 text-white font-semibold py-3.5 px-6 rounded-lg shadow-lg disabled:opacity-60 disabled:cursor-not-allowed transition-all duration-300 ease-in-out transform focus:outline-none focus:ring-4 focus:ring-sky-500/50"
                        whileHover={{ scale: (isLoadingAnalysis || !isAppConfigLoaded) ? 1 : 1.03, y: (isLoadingAnalysis || !isAppConfigLoaded) ? 0 : -2 }}
                        whileTap={{ scale: (isLoadingAnalysis || !isAppConfigLoaded) ? 1 : 0.98 }}
                    >
                        {isLoadingAnalysis ? <LoadingSpinner /> : 'Analyze Review'}
                    </motion.button>
                    {!isAppConfigLoaded && <p className="text-xs text-yellow-500 mt-3 text-center animate-pulse">Waiting for app configuration...</p>}
                </motion.section>

                {/* Right Column */}
                <section className="lg:col-span-8 space-y-8 lg:space-y-10">
                    <AnimatePresence>
                        {analysisResult && !backendError && (
                            <AnimatedSection className="bg-slate-800/60 backdrop-blur-xl p-6 sm:p-8 rounded-2xl shadow-2xl border border-slate-700/50">
                                <div className="flex items-center mb-5">
                                    <FiBarChart2 className="h-8 w-8 text-teal-400 mr-3" />
                                    <h2 className="text-3xl font-semibold text-teal-300">Analysis Result</h2>
                                </div>
                                <div className="space-y-3 text-lg">
                                    <p>Sentiment:
                                        <motion.span key={analysisResult.sentiment}
                                            className={`font-bold ml-2 px-3 py-1 rounded-full text-sm ${
                                                analysisResult.sentiment === 'Positive' ? 'bg-green-500/20 text-green-300 border border-green-500/50' :
                                                analysisResult.sentiment === 'Negative' ? 'bg-red-500/20 text-red-300 border border-red-500/50' :
                                                'bg-yellow-500/20 text-yellow-300 border border-yellow-500/50'
                                            }`}
                                            initial={{ opacity: 0, scale: 0.5 }} animate={{ opacity: 1, scale: 1 }} transition={{ type: "spring", stiffness: 300, damping: 15 }}
                                        > {analysisResult.sentiment} </motion.span>
                                    </p>
                                    <p>Predicted Rating:
                                        <motion.span key={analysisResult.pseudo_rating} className="font-bold ml-2 text-amber-300 text-xl"
                                            initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.2 }}
                                        > {analysisResult.pseudo_rating}<span className="text-sm text-slate-400">/10</span> </motion.span>
                                    </p>
                                </div>
                            </AnimatedSection>
                        )}
                    </AnimatePresence>

                    {backendError && (!analysisResult || (analysisResult && backendError)) && (
                         <AnimatedSection className="bg-red-800/30 backdrop-blur-md p-6 rounded-xl shadow-lg border border-red-700/50">
                            <div className="flex items-center">
                                <FiAlertTriangle className="h-7 w-7 text-red-400 mr-3 flex-shrink-0" />
                                <div>
                                    <p className="font-semibold text-red-300 text-xl">Application Error</p>
                                    <p className="text-red-200 mt-1">{backendError}</p>
                                </div>
                            </div>
                        </AnimatedSection>
                    )}

                    <AnimatedSection className="bg-slate-800/60 backdrop-blur-xl p-6 sm:p-8 rounded-2xl shadow-2xl border border-slate-700/50">
                         <div className="flex items-center mb-5">
                            <FiClock className="h-8 w-8 text-purple-400 mr-3" />
                            <h2 className="text-3xl font-semibold text-purple-300">Recent Reviews for {selectedMovie}</h2>
                        </div>
                        {isLoadingHistory ? (<div className="py-10"><LoadingSpinner /></div>)
                            : movieHistory.length > 0 ? (
                                <ul className="space-y-5 max-h-[32rem] overflow-y-auto pr-3 custom-scrollbar">
                                    {movieHistory.map((item, index) => (
                                        <motion.li key={item.id || index} className="p-5 bg-slate-700/70 rounded-xl shadow-lg border border-slate-600/50"
                                            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4, delay: index * 0.07, ease: "easeOut" }} >
                                            <p className="text-md text-slate-200 italic mb-3 leading-relaxed break-words">"{item.review_text}"</p>
                                            <div className="flex justify-between items-center text-sm pt-2 border-t border-slate-600">
                                                <span className={`font-semibold px-2 py-0.5 rounded-full text-xs ${
                                                    item.predicted_sentiment === 'Positive' ? 'bg-green-500/20 text-green-300 border border-green-500/30' :
                                                    item.predicted_sentiment === 'Negative' ? 'bg-red-500/20 text-red-300 border border-red-500/30' :
                                                    'bg-yellow-500/20 text-yellow-300 border border-yellow-500/30'}`}>
                                                    {item.predicted_sentiment}
                                                </span>
                                                <span className="text-amber-400 font-medium">Rating: {item.pseudo_rating}/10</span>
                                            </div>
                                        </motion.li>
                                    ))}
                                </ul>
                            ) : (
                                <motion.p initial={{opacity:0}} animate={{opacity:1}} className="text-slate-400 text-center py-10 text-lg">
                                    {(backendError && movieHistory.length === 0 && !isLoadingHistory && !analysisResult) ? "Could not load history due to an error." : `No reviews yet for ${selectedMovie}. Be the first to share your thoughts!`}
                                </motion.p>
                            )}
                    </AnimatedSection>
                </section>
            </div>
            <footer className="text-center mt-16 py-8 border-t border-slate-700/50">
                <p className="text-sm text-slate-500">© {new Date().getFullYear()} Movie Analyzer Demo. Built for learning purposes.</p>
            </footer>
        </div>
    );
}
export default App;