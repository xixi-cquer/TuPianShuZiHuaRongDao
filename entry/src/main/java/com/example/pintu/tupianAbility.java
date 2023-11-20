package com.example.pintu;

import ohos.agp.render.Canvas;
import ohos.agp.render.Paint;
import ohos.agp.render.Texture;
import ohos.agp.utils.Color;
import ohos.app.Context;
import ohos.global.resource.Resource;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.media.image.common.PixelFormat;
import ohos.media.image.common.Rect;
import ohos.media.image.common.Size;

import java.util.Vector;

public class tupianAbility {
    public static Vector<PixelMap> pixelMaps = new Vector<PixelMap>();

    //将media图片转换成PixMap
    public static PixelMap getPixelMap(Context context, int imgRes) {
        byte[] imgData = null;
        try {
            Resource imgResource = context.getResourceManager().getResource(imgRes);
            int imgLength = imgResource.available();
            imgData = new byte[imgLength];
            imgResource.read(imgData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (imgData == null)
            return null;
        ImageSource imageSource = ImageSource.create(imgData, null);
        return imageSource.createPixelmap(null);
    }

    public static Vector<PixelMap> Split(Context context, PixelMap p, int picLen) {
        pixelMaps.clear();
        //获取图片的像素数量
        long total = p.getPixelBytesNumber() / 4;
        int width = p.getBytesNumberPerRow() / 4;
        int height = (int) total / width;

//        获取分割之后的图片像素信息
        int new_width = width / picLen;
        int new_height = height / picLen;

        //设置InitializationOptions
        PixelMap.InitializationOptions initializationOptions = new PixelMap.InitializationOptions();
        initializationOptions.size = new Size(new_width, new_height);
        initializationOptions.pixelFormat = PixelFormat.ARGB_8888;
        initializationOptions.editable = true;

        //读取指定区域的像素信息
        int[] pixelArray = new int[new_width * new_height];
        int t=0;
        for (int i = 0; i < picLen; i++) {
            for (int j = 0; j < picLen; j++) {
                Rect region = new Rect(j * new_width, i * new_height, new_width, new_height);
                p.readPixels(pixelArray, 0, new_width, region);
                PixelMap temp = PixelMap.create(pixelArray, initializationOptions);

                Canvas canvas = new Canvas(new Texture(temp));
                Paint paint = new Paint();
                paint.setTextSize(100);
                paint.setColor(Color.RED);
                t++;
                canvas.drawText(paint, "" + t, new_width/2, new_height/2);

                pixelMaps.add(temp);
            }
        }
//        pixelMaps.remove(picLen*picLen-1);
        return pixelMaps;
    }
}
