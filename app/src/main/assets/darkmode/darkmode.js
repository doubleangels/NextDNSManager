console.log('Dark mode script loaded.');

addEventListener("DOMContentLoaded", (event) => {
    console.log('Removing styles.');
    const stylesheets = document.querySelectorAll('link[rel="stylesheet"]');
    stylesheets.forEach(sheet => sheet.remove());
});
