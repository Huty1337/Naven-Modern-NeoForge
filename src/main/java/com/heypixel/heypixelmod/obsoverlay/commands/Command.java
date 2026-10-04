package com.heypixel.heypixelmod.obsoverlay.commands;

public abstract class Command {
   private String name;
   private String description;
   private String[] aliases;

   protected void initCommand() {
      if (this.getClass().isAnnotationPresent(CommandInfo.class)) {
         CommandInfo commandInfo = this.getClass().getAnnotation(CommandInfo.class);
         this.name = commandInfo.name();
         this.description = commandInfo.description();
         this.aliases = commandInfo.aliases();
      }
   }

   public abstract void onCommand(String[] var1);

   public abstract String[] onTab(String[] var1);

   public Command(String name, String description, String[] aliases) {
      this.name = name;
      this.description = description;
      this.aliases = aliases;
   }

   public Command() {
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public String[] getAliases() {
      return this.aliases;
   }
}
