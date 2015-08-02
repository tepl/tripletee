for res = {'160','320'};
    for item = {'cross','nought','splash'}
        nRows = 1;
        if(strcmp(item{1},'splash')==1)
            nCols = 30;
        else
            nCols = 15;
        end
        imgCell = cell(nRows,nCols);
        alpCell = cell(nRows,nCols);

        for iImage = 1:nRows*nCols
            imageName = sprintf('%s/%s/%04i.png',res{1},item{1},iImage);
            [imgCell{iImage},map,alpCell{iImage}] = imread(imageName);
        end

        bigImage = cell2mat(imgCell);
        alpImage = cell2mat(alpCell);
        imageName = sprintf('final/%s%s.png',item{1},res{1});
        imwrite(bigImage,imageName,'png','Alpha',alpImage);
    end
end