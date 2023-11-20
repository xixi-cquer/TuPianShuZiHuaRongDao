package com.example.pintu.slice;

import com.example.pintu.ResourceTable;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.agp.components.Text;
import ohos.global.resource.Resource;

public class MainAbilitySlice extends AbilitySlice {
    private Text t;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_main);

        t = (Text) findComponentById(ResourceTable.Id_text_helloworld);
        t.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                Intent i = new Intent();
                present(new GameAbilitySlice(), i);
            }
        });

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
