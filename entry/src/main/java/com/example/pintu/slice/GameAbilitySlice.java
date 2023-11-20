package com.example.pintu.slice;

import com.example.pintu.ImageListProvider;
import com.example.pintu.tupianAbility;
import com.example.pintu.ResourceTable;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.ability.DataAbilityHelper;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.animation.AnimatorGroup;
import ohos.agp.animation.AnimatorProperty;
import ohos.agp.components.*;
import ohos.agp.components.element.PixelMapElement;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.utils.Color;
import ohos.agp.utils.LayoutAlignment;
import ohos.agp.utils.TextAlignment;
import ohos.agp.window.dialog.CommonDialog;
import ohos.agp.window.dialog.IDialog;
import ohos.data.rdb.ValuesBucket;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;
import ohos.media.image.ImagePacker;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.media.image.common.Size;
import ohos.media.photokit.metadata.AVStorage;
import ohos.utils.net.Uri;

import static ohos.agp.components.ComponentContainer.LayoutConfig.MATCH_CONTENT;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class GameAbilitySlice extends AbilitySlice { //组件
    private final Vector<Image> imagesBlockList = new Vector<Image>();  //拼图块

    private TickTimer tickTimer;  //计时器
    private Image present_image;  //预览图
    private TableLayout secondLayout;
    private long startTime, currentTime;  // 开始时间
    private Text countText_count; // 点击次数
    private int picLen = 3, clickCount = 0;  //拼图行数、列数
    private int phone_width, phone_height;
    private int click_index_in_list = -1, blank_index_in_list = -1;
    private boolean isRunning = false, isMoving = false, isPicChange = false;
    private Vector<PixelMap> imgBlockVector;  // 记录正确拼图块顺序
    private int click_pos;
    private int blank_pos;
    private int blank_pos_row;
    private int blank_pos_col;
    private int playing_image_id; // 当前游戏的图片

    public GameAbilitySlice() {
    }

    @Override
    public void onStart(Intent intent) {
        phone_width = AttrHelper.vp2px(getContext().getResourceManager().getDeviceCapability().width, this);
        phone_height = AttrHelper.vp2px(getContext().getResourceManager().getDeviceCapability().height, this);
        playing_image_id = intent.getIntParam("playing_image_id", ResourceTable.Media_p1);

        blank_pos = picLen * picLen - 1;
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_abilitygame);
        initLayout();
        initParams();
        initImages();
//        initAlbum();
        ListContainer lcImgList= findComponentById(ResourceTable.Id_ListContainer);
        List<Integer> imgList=getImageList();
        ImageListProvider provider=new ImageListProvider(imgList,this);
        provider.setListener(new ImageListProvider.ClickedListener() {
            @Override
            public void click(int pos) {
                present_image.setPixelMap(imgList.get(pos));
                playing_image_id = imgList.get(pos);
                StartGame();
            }
        });
        lcImgList.setItemProvider(provider);
        StartGame();
    }

    private void initImages() {
        PixelMap p;
        if (isPicChange)
            p = present_image.getPixelMap();
        else
            p = tupianAbility.getPixelMap(getContext(), playing_image_id);

        this.present_image.setPixelMap(p);// 当前完整图片
        imgBlockVector = tupianAbility.Split(getContext(), p, picLen);

        initBlockImages();
    }

    //初始化布局
    public void initLayout() {
        int textSize = (int) (this.phone_width * 0.06);

        //主布局
        DirectionalLayout mainLayout = new DirectionalLayout(this);
        mainLayout.setOrientation(Component.VERTICAL);
        mainLayout.setWidth(ComponentContainer.LayoutConfig.MATCH_PARENT);
        mainLayout.setHeight(ComponentContainer.LayoutConfig.MATCH_PARENT);
        mainLayout.setAlignment(1);

        //第一部分，图片预览，计时器，StartButton， ResetButton，ChangeModelButton
        //添加计时器
        DirectionalLayout timer = new DirectionalLayout(this);
        timer.setWidth(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        timer.setHeight(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        timer.setOrientation(Component.HORIZONTAL);
        timer.setMarginTop(80);
        //text
        Text t = new Text(this);
        t.setWidth(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        t.setHeight(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        t.setText("用时: ");
        t.setTextSize(textSize);
        timer.addComponent(t);

        this.tickTimer = new TickTimer(this);
        this.tickTimer.setWidth(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        this.tickTimer.setHeight(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        this.tickTimer.setFormat("mm分ss秒");
        this.tickTimer.setCountDown(false);
        this.tickTimer.setTextSize(textSize);
        timer.addComponent(tickTimer);

        Text countText_text = new Text(this);
        countText_text.setWidth(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        countText_text.setHeight(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        countText_text.setText("   步数: ");
        countText_text.setTextSize(textSize);

        countText_count = new Text(this);
        countText_count.setWidth(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        countText_count.setHeight(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        countText_count.setText("0");
        countText_count.setTextSize(textSize);

        timer.addComponent(countText_text);
        timer.addComponent(countText_count);
        mainLayout.addComponent(timer);

        //第二部分，拼图块放置
        secondLayout = new TableLayout(this);
        secondLayout.setOrientation(Component.HORIZONTAL);
        secondLayout.setWidth(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        secondLayout.setHeight(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        secondLayout.setMarginTop(80);


        mainLayout.addComponent(secondLayout);

        DirectionalLayout butLayout = new DirectionalLayout(this);
        butLayout.setWidth(MATCH_CONTENT);
        butLayout.setHeight(MATCH_CONTENT);
        butLayout.setOrientation(Component.HORIZONTAL);
        butLayout.setMarginTop(40);

        Button easyButton = new Button(this);
        easyButton.setText("简单");
        easyButton.setWidth(MATCH_CONTENT);
        easyButton.setHeight(MATCH_CONTENT);
        easyButton.setMarginLeft(20);
        easyButton.setTextSize(60);
        easyButton.setBackground(new ShapeElement(getContext(), ResourceTable.Graphic_btnbackground));
        easyButton.setClickedListener(component -> {
            picLen = 2;
            StartGame();
        });

        Button mediumButton = new Button(this);
        mediumButton.setText("普通");
        mediumButton.setWidth(MATCH_CONTENT);
        mediumButton.setHeight(MATCH_CONTENT);
        mediumButton.setMarginLeft(20);
        mediumButton.setBackground(new ShapeElement(getContext(), ResourceTable.Graphic_btnbackground));
        mediumButton.setHintColor(Color.BLUE);
        mediumButton.setTextSize(60);
        mediumButton.setClickedListener(component -> {
            picLen = 3;
            StartGame();
        });

        Button hardButton = new Button(this);
        hardButton.setText("困难");
        hardButton.setWidth(MATCH_CONTENT);
        hardButton.setHeight(MATCH_CONTENT);
        hardButton.setMarginLeft(20);
        hardButton.setBackground(new ShapeElement(getContext(), ResourceTable.Graphic_btnbackground));
        hardButton.setTextSize(60);
        hardButton.setClickedListener(component -> {
            picLen = 4;
            StartGame();
        });

        Button expertButton = new Button(this);
        expertButton.setText("超难");
        expertButton.setWidth(MATCH_CONTENT);
        expertButton.setHeight(MATCH_CONTENT);
        expertButton.setMarginLeft(20);
        expertButton.setBackground(new ShapeElement(getContext(), ResourceTable.Graphic_btnbackground));
        expertButton.setTextSize(60);
        expertButton.setClickedListener(component -> {
            picLen = 5;
            StartGame();
        });

        
        butLayout.addComponent(easyButton);
        butLayout.addComponent(mediumButton);
        butLayout.addComponent(hardButton);
        butLayout.addComponent(expertButton);

        mainLayout.addComponent(butLayout);



        //基础信息
        DirectionalLayout firstLayout = new DirectionalLayout(this);
        firstLayout.setWidth((int) (phone_width * 0.8));
        firstLayout.setHeight(ComponentContainer.LayoutConfig.MATCH_CONTENT);
        firstLayout.setOrientation(Component.HORIZONTAL);
        firstLayout.setAlignment(1);
        firstLayout.setMarginTop(50);

        //添加计时器，StartButton， ResetButton
        DirectionalLayout btnDirectionLayout = new DirectionalLayout(getContext());
        btnDirectionLayout.setWidth(MATCH_CONTENT);
        btnDirectionLayout.setHeight(MATCH_CONTENT);
        btnDirectionLayout.setOrientation(Component.HORIZONTAL);
        btnDirectionLayout.setAlignment(LayoutAlignment.HORIZONTAL_CENTER);
        btnDirectionLayout.setMarginTop(20);

        //添加StartButton
        //重置游戏按钮
        Button startBtn = new Button(this);
        startBtn.setWidth(MATCH_CONTENT);
        startBtn.setHeight(MATCH_CONTENT);
        startBtn.setText("重新开始");
        startBtn.setTextAlignment(TextAlignment.CENTER);
        startBtn.setTextSize(100);
        startBtn.setMarginTop(20);
        startBtn.setBackground(new ShapeElement(getContext(), ResourceTable.Graphic_btnbackground));
        startBtn.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                StartGame();
            }
        });
        btnDirectionLayout.addComponent(startBtn);

        Button endBtn = new Button(this);
        endBtn.setWidth(MATCH_CONTENT);
        endBtn.setHeight(MATCH_CONTENT);
        endBtn.setText("结束游戏");
        endBtn.setMarginTop(20);
        endBtn.setMarginLeft(30);
        endBtn.setTextAlignment(TextAlignment.CENTER);
        endBtn.setTextSize(100);
        endBtn.setBackground(new ShapeElement(getContext(), ResourceTable.Graphic_btnbackground));
        endBtn.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                endGame(present_image);
                StartGame();
            }
        });
        btnDirectionLayout.addComponent(endBtn);

        firstLayout.addComponent(btnDirectionLayout);
        mainLayout.addComponent(firstLayout);

        //添加预览图
        this.present_image = new Image(this);
        this.present_image.setWidth((int) (phone_width / 2 * 0.8));
        this.present_image.setHeight((int) (phone_width / 2 * 0.8));
        this.present_image.setScaleMode(Image.ScaleMode.STRETCH);
        this.present_image.setCornerRadius((float) 40);
        DirectionalLayout total = (DirectionalLayout) findComponentById(ResourceTable.Id_backgroundLayout);
        total.addComponent(mainLayout);

    }

    private void initBlockImages() {
        secondLayout.setColumnCount(this.picLen);
        secondLayout.setRowCount(this.picLen);
        secondLayout.removeAllComponents();
        imagesBlockList.clear();
        int tableLayoutWidth = (int) (phone_width * 0.8);
        //信息
        int partWidth = (int) (tableLayoutWidth / this.picLen);
        int partMargin = (int) (partWidth * 0.01);


        for (int i = 0; i < this.picLen; i++) {
            for (int j = 0; j < this.picLen; j++) {
                Image part = new Image(this);
                part.setWidth(partWidth);
                part.setHeight(partWidth);
                part.setId(i * picLen + j);
                part.setScaleMode(Image.ScaleMode.STRETCH);
                part.setMarginsTopAndBottom(partMargin, partMargin);
                part.setMarginsLeftAndRight(partMargin, partMargin);
                this.imagesBlockList.add(part);
                secondLayout.addComponent(part);
            }
        }
        for (Image image : imagesBlockList) {
            image.setClickedListener(new Component.ClickedListener() {
                @Override
                public void onClick(Component component) {
                    ImageBlockClick(component);
                }
            });
        }

        // 展示图片
        for (int i = 0; i < imgBlockVector.size(); i++) {
            imagesBlockList.get(i).setPixelMap(imgBlockVector.get(i));
            imagesBlockList.get(i).setVisibility(Component.VISIBLE);
        }
    }


    private void initParams() {
        isRunning = false;
        resetTime();
        resetStep();
        for (Image image : imagesBlockList) {
            image.setVisibility(Component.VISIBLE);
        }
        blank_pos_row = blank_pos_col = picLen * picLen - 1;
    }


    // 开始游戏按钮
    private void StartGame() {
        // 初始化可见文字参数
        initParams();
        // 将图片的id与块绑定
        // 更新imgBlockVector的图片列表
        initImages();
        // 打乱图片
        shuffleImageBlocks();
        isRunning = true;
        startTime();
    }



    private void ImageBlockClick(Component component) {
        if (!isMoving && isRunning) {
            isMoving = true;
            int tableLayoutWidth = (int) (phone_width * 0.8);
            int partWidth = tableLayoutWidth / picLen;
            int partMargin = (int) (partWidth * 0.01);

            //当前点击的Image相对位置
            click_pos = component.getId();
            for (int i = 0; i < imgBlockVector.size(); i++) {
                if (imagesBlockList.get(i).getId() == click_pos) {
                    click_index_in_list = i;
                }
                if (imagesBlockList.get(i).getId() == blank_pos) {
                    blank_index_in_list = i;
                }
            }
            Image clickImage = imagesBlockList.get(click_index_in_list);
            Image blankImage = imagesBlockList.get(blank_index_in_list);
            int click_pos_row = click_pos / picLen;
            // 记录空白块的位置
            int click_pos_col = click_pos % picLen;

            //空白块的位置
            blank_pos_row = blank_pos / picLen;
            blank_pos_col = blank_pos % picLen;

            //确认相邻,相邻则交换背景
            if (Math.abs(click_pos_row - blank_pos_row) + Math.abs(click_pos_col - blank_pos_col) == 1) {
                // 交换两个图片在数组中的位置
                blankImage.setId(click_pos);
                clickImage.setId(blank_pos);
                Collections.swap(imagesBlockList, click_index_in_list, blank_index_in_list);


                AnimatorProperty animatorProperty = new AnimatorProperty();
                animatorProperty.setTarget(clickImage);
                AnimatorProperty animatorProperty2 = new AnimatorProperty();
                animatorProperty2.setTarget(blankImage);

                AnimatorGroup animatorGroup = new AnimatorGroup();
                animatorProperty.moveFromX(click_pos_col * (partWidth + partMargin * 2)).moveToX(blank_pos_col * (partWidth + partMargin * 2));
                animatorProperty.moveFromY(click_pos_row * (partWidth + partMargin * 2)).moveToY(blank_pos_row * (partWidth + partMargin * 2));
                animatorProperty2.moveFromX(blank_pos_col * (partWidth + partMargin * 2)).moveToX(click_pos_col * (partWidth + partMargin * 2));
                animatorProperty2.moveFromY(blank_pos_row * (partWidth + partMargin * 2)).moveToY(click_pos_row * (partWidth + partMargin * 2));
                animatorProperty.setDuration(50);
                animatorProperty2.setDuration(50);
                animatorGroup.runParallel(animatorProperty, animatorProperty2);
                animatorGroup.start();
                blank_pos = click_pos;


                clickCount += 1;
                countText_count.setText(String.valueOf(clickCount));
                checkComplete();
            }
            isMoving = false;
        }
    }

    private void checkComplete() {
        boolean complete = true;
        for (Image image : imagesBlockList) {
            if (image.getId() != (Integer) image.getTag()) {
                complete = false;
                break;
            }
        }
        if (complete) {
            isRunning = false;
            tickTimer.stop();
            showDialogSuccess();
        }
    }

    private void shuffleImageBlocks() {
        Vector<Integer> indexList = new Vector<>();
        for (int i = 0; i < imgBlockVector.size(); i++)
            indexList.add(i);

        // 更新图片
        // 生成有解的随机数列
        int n = 1;
        while (n % 2 == 1) {
            n = 0;
            Collections.shuffle(indexList);
            // 判断是否有解
            for (int i = 0; i < indexList.size() - 1; i++) {

                if (indexList.get(i) > indexList.get(i + 1))
                    n++;
            }
            if (n % 2 == 1) continue;
            imagesBlockList.clear();
            initBlockImages();
            for (int i = 0; i < imgBlockVector.size(); i++) {
                if (indexList.get(i) == picLen * picLen - 1) {
                    blank_pos = i;
                    imagesBlockList.get(blank_pos).setVisibility(Component.INVISIBLE);
                }
                imagesBlockList.get(i).setTag(indexList.get(i));
                imagesBlockList.get(i).setPixelMap(imgBlockVector.get(indexList.get(i)));
            }
        }

    }

    private void resetStep() {
        clickCount = 0;
        countText_count.setText("0");
    }


    //拼图成功对话框
    public void showDialogSuccess() {
        CommonDialog cd = new CommonDialog(this);
        cd.setTitleText("完成拼图");
        String passtime = this.tickTimer.getText();
        cd.setContentText("  用时：" + passtime+"  步数：" + this.countText_count.getText());
        cd.setSize(800, MATCH_CONTENT);
        cd.setButton(1, "确认", new IDialog.ClickedListener() {
            @Override
            public void onClick(IDialog iDialog, int i) {
                cd.destroy();
            }
        });
        cd.setAutoClosable(true);
        cd.show();
    }

    public void endGame(Image present_image) {
        CommonDialog cd = new CommonDialog(this);
        cd.setTitleText("正确答案");


        PixelMapElement element = new PixelMapElement(present_image.getPixelMap());

        Image popupImage = new Image(this);
        popupImage.setHeight(800);
        popupImage.setWidth(800);
        popupImage.setImageElement(element);
        cd.setContentCustomComponent(popupImage);
        cd.setButton(1, "确认", new IDialog.ClickedListener() {
            @Override
            public void onClick(IDialog iDialog, int i) {
                cd.destroy();
            }
        });
        cd.setSize(1000,MATCH_CONTENT);
        cd.show();
    }




    //开始计时
    public void startTime() {
        this.startTime = 0;
        this.currentTime = System.currentTimeMillis();
        this.tickTimer.setBaseTime(currentTime - startTime);
        this.tickTimer.start();
    }

    //重置时间
    public void resetTime() {
        tickTimer.stop();
        this.tickTimer.setBaseTime(0);
        this.startTime = 0;
        this.currentTime = 0;
    }

    public List<Integer> getImageList(){
        List<Integer> lst =new ArrayList<>();
        lst.add(ResourceTable.Media_p1);
        lst.add(ResourceTable.Media_p2);
        lst.add(ResourceTable.Media_p3);
        lst.add(ResourceTable.Media_p4);
        lst.add(ResourceTable.Media_p1);
        lst.add(ResourceTable.Media_p1);
        return lst;
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
    }
}
