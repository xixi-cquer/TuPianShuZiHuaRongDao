package com.example.pintu;

import ohos.agp.components.*;
import ohos.app.Context;

import java.util.List;

public class ImageListProvider extends BaseItemProvider{
    List<Integer>imgList;
    Context ctx;
    ClickedListener listener;
    public void setListener(ClickedListener listener){
        this.listener=listener;
    }
    public static interface ClickedListener{
        void click(int pos);
    }
    @Override
    public int getCount() {
        return imgList.size();
    }

    @Override
    public Object getItem(int i) {
        return imgList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public ImageListProvider(List<Integer> imgList, Context ctx){
        this.imgList=imgList;
        this.ctx=ctx;
    }
    @Override
    public Component getComponent(int i, Component component, ComponentContainer componentContainer) {
        DirectionalLayout dl=(DirectionalLayout) LayoutScatter.getInstance(ctx).parse(ResourceTable.Layout_imglist_item,null,false);
        Image img=(Image) dl.findComponentById((ResourceTable.Id_imglist_item));
        img.setImageAndDecodeBounds(imgList.get(i));
        img.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                listener.click(i);
            }
        });
        return dl;
    }
}