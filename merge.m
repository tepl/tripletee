nRows = 1;
nCols = 15;
imgCell = cell(nRows,nCols);

for iImage = 1:nRows*nCols
    imageName = sprintf('320/nought/%04i.png',iImage);
    imgCell{iImage} = imread(imageName);
end

bigImage = cell2mat(imgCell);
imwrite(bigImage,'320/final/nought320.png')