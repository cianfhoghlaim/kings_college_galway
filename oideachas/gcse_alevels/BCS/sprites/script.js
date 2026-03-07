// MakeCode Arcade pixel art converter
const imageFiles = [
    'black_panther.png',
    'dr_strange.png',
    'groot.png',
    'hela.png',
    'hulk.png',
    'ironman.png',
    'loki.png',
    'luna.png',
    'magik.png',
    'magneto.png',
    'mantis.png',
    'peni_parker.png',
    'punisher.png',
    'rocket_raccoon.png',
    'scarlet_witch.png',
    'spiderman.png',
    'starlord.png',
    'storm.png'
];

// Retro game color palette
const palette = [
    [0, 0, 0],       // Black
    [255, 255, 255], // White
    [255, 0, 0],     // Red
    [0, 255, 0],     // Green
    [0, 0, 255],     // Blue
    [255, 255, 0],   // Yellow
    [255, 128, 0],   // Orange
    [128, 0, 128]    // Purple
];

// Wait for Pixelit to load
const checkPixelit = setInterval(() => {
    if (typeof Pixelit !== 'undefined') {
        clearInterval(checkPixelit);
        processAllImages();
    }
}, 100);

async function processAllImages() {
    const outputContainer = document.createElement('div');
    outputContainer.style.display = 'grid';
    outputContainer.style.gridTemplateColumns = 'repeat(auto-fill, minmax(160px, 1fr))';
    outputContainer.style.gap = '20px';
    document.body.append(outputContainer);

    for (const filename of imageFiles) {
        await processImage(filename, outputContainer);
    }
}

function processImage(filename, container) {
    return new Promise((resolve) => {
        const img = new Image();
        img.src = filename;

        img.onload = () => {
            const canvas = document.createElement('canvas');
            canvas.width = 16;
            canvas.height = 16;

            // Create pixelit instance
            const pixelit = new Pixelit({
                from: img,
                to: canvas,
                scale: 16, // Scale to 16x16
                palette: palette
            });

            // Convert to pixel art
            pixelit.draw().convertToPalette();

            // Create preview
            const previewContainer = document.createElement('div');
            previewContainer.style.textAlign = 'center';

            const preview = document.createElement('img');
            preview.src = canvas.toDataURL('image/png');
            preview.style.width = '160px';
            preview.style.imageRendering = 'pixelated';

            const downloadLink = document.createElement('a');
            downloadLink.href = preview.src;
            downloadLink.download = `arcade_${filename}`;
            downloadLink.textContent = `Download ${filename}`;
            downloadLink.style.display = 'block';
            downloadLink.style.marginTop = '10px';

            previewContainer.append(preview, downloadLink);
            container.append(previewContainer);

            resolve();
        };
    });
}
