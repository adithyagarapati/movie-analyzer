// src/movies.js
export const movies = [
    { name: "Animal", image: "/images/animal.jpg" },
    { name: "Bahubali", image: "/images/bahubali.jpg" },
    { name: "Jailer", image: "/images/jailer.jpg" },
    { name: "Kalki 2898 AD", image: "/images/kalki.jpg" },
    { name: "Kantara", image: "/images/kantara.jpg" },
    { name: "KGF", image: "/images/kgf.jpg" },
    { name: "Manjummel Boys", image: "/images/manjummel_boys.jpg" },
    { name: "Pushpa", image: "/images/pushpa.jpg" },
    { name: "RRR", image: "/images/rrr.jpg" },
    { name: "Vikram", image: "/images/vikram.jpg" },
].sort((a, b) => a.name.localeCompare(b.name));