nRows = 1;
nCols = 15;
imgCell = cell(nRows,nCols);

for iImage = 1:nRows*nCols
    imageName = sprintf('splash/%04i.png',iImage);
    imgCell{iImage} = imread(imageName);
end

bigImage = cell2mat(imgCell);
imwrite(bigImage,'final/splash.png')