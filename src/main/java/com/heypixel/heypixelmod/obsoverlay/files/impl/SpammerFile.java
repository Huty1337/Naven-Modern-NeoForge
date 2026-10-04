package com.heypixel.heypixelmod.obsoverlay.files.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.files.ClientFile;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Spammer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class SpammerFile extends ClientFile {
   private static final String[] styles = new String[]{
      "我是狂笑的蛇陈安健，我是花雨庭反作弊兼Hack Lunar端制作者！",
      "我是狂笑的蛇陈安健，我使用Hack Lunar不会Ban！因为我制作了花雨庭反作弊！",
      "你知道吗，我是狂笑的蛇陈安健！我制作了花雨庭反作弊，误封你们！",
      "不如我狂笑的蛇将写散文制作的Hack Lunar，我很稳定因为花雨庭反作弊就是出自本人之手！",
      "兄弟，你听我说！我不会被封，因为我是狂笑的蛇，我制作了花雨庭反作弊！",
      "我是狂笑的蛇陈安健，我疯狂巴结花雨庭的技术！",
      "陈安健制作的Hack Lunar依然稳定绕过花雨庭，因为我是狂笑的蛇！",
      "我是大名鼎鼎的狂笑的蛇将写散文本人陈安健，我能无视反作弊飘主播因为我制作了花雨庭反作弊！",
      "羡慕我不会被封吗？因为我狂笑的蛇本人陈安健制作了花雨庭的反作弊！",
      "B站搜索狂笑的蛇将写散文，看我陈安健精彩集锦！",
      "我狂笑的蛇陈安健能无视反作弊，因为花雨庭反作弊就是我做的！",
      "哈哈你知道你为什么会被误封吗？因为我狂笑的蛇陈安健制作了花雨庭反作弊！"
   };

   public SpammerFile() {
      super("spammers.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      Spammer module = (Spammer)Naven.getInstance().getModuleManager().getModule(Spammer.class);
      List<BooleanValue> values = module.getValues();

      String line;
      while ((line = reader.readLine()) != null) {
         values.add(ValueBuilder.create(module, line).setDefaultBooleanValue(false).build().getBooleanValue());
      }

      if (values.isEmpty()) {
         for (String style : styles) {
            values.add(ValueBuilder.create(module, style).setDefaultBooleanValue(false).build().getBooleanValue());
         }
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      Spammer module = (Spammer)Naven.getInstance().getModuleManager().getModule(Spammer.class);

      for (BooleanValue value : module.getValues()) {
         writer.write(value.getName() + "\n");
      }
   }
}
