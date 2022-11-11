package traben.entity_texture_features.config.screens;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import traben.entity_texture_features.ETFClientCommon;
import traben.entity_texture_features.ETFVersionDifferenceHandler;
import traben.entity_texture_features.utils.ETFUtils2;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static traben.entity_texture_features.ETFClientCommon.CONFIG_DIR;

//inspired by puzzles custom gui code
public class ETFConfigScreenSkinToolOutcome extends ETFConfigScreen {
    private final boolean didSucceed;

    protected ETFConfigScreenSkinToolOutcome(Screen parent, boolean success) {
        super(ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".player_skin_editor.print_skin.result"), parent);
        didSucceed = success;
    }

    //upload code sourced from by https://github.com/cobrasrock/Skin-Swapper/blob/1.18-fabric/src/main/java/net/cobrasrock/skinswapper/changeskin/SkinChange.java
    //I do not intend to allow uploading of just any skin file, only ETF skin feature changes to an already existing skin, so I will not encroach on the scope of the excellent skin swapper mod
    public static boolean uploadSkin(boolean skinType) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        try {
            if ("127.0.0.1".equals(InetAddress.getLocalHost().getHostAddress())) {
                return false;
            }

            String auth = MinecraftClient.getInstance().getSession().getAccessToken();

            //uploads skin
            HttpPost http = new HttpPost("https://api.minecraftservices.com/minecraft/profile/skins");


            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("variant", skinType ? "classic" : "slim", ContentType.TEXT_PLAIN);
            assert CONFIG_DIR != null;
            builder.addBinaryBody(
                    "file",
                    Files.newInputStream(new File(CONFIG_DIR.getParent(), "\\ETF_player_skin_printout.png").toPath()),
                    ContentType.IMAGE_PNG,
                    "skin.png"
            );

            http.setEntity(builder.build());
            http.addHeader("Authorization", "Bearer " + auth);
            HttpResponse response = httpClient.execute(http);

            return response.getStatusLine().getStatusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void init() {
        super.init();


        this.addButton(getETFButton((int) (this.width * 0.55), (int) (this.height * 0.9), (int) (this.width * 0.2), 20,
                ScreenTexts.DONE,
                (button) -> Objects.requireNonNull(client).openScreen(parent)));
        if (didSucceed) {
            this.addButton(getETFButton((int) (this.width * 0.15), (int) (this.height * 0.6), (int) (this.width * 0.7), 20,
                    ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".player_skin_editor.print_skin.open"),
                    (button) -> {
                        try {
                            @SuppressWarnings("ConstantConditions")
                            File outputDirectory = new File(CONFIG_DIR.getParent());
                            Util.getOperatingSystem().open(outputDirectory);
                        } catch (Exception ignored) {
                        }
                    }));
            this.addButton(getETFButton((int) (this.width * 0.15), (int) (this.height * 0.4), (int) (this.width * 0.7), 20,
                    ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".player_skin_editor.upload_skin"),
                    (button) -> {
                        boolean skinType = true;//true for steve false for alex
                        if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().getNetworkHandler() != null) {
                            PlayerListEntry playerListEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(MinecraftClient.getInstance().player.getUuid());
                            if (playerListEntry != null) {
                                String skinTypeData = MinecraftClient.getInstance().getSkinProvider().getTextures(playerListEntry.getProfile()).get(MinecraftProfileTexture.Type.SKIN).getMetadata("model");
                                if (skinTypeData != null) {
                                    skinType = !skinTypeData.equals("slim");
                                }
                            }
                        }
                        boolean changeSuccess = uploadSkin(skinType);
                        button.setMessage(ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".player_skin_editor.upload_skin." +
                                (changeSuccess ? "success" : "fail")));
                        if(changeSuccess){
                            ETFUtils2.logWarn(ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".player_skin_editor.upload_skin.success" ).getString()
                                    ,true);
                        }
                        button.active = false;
                    }));
        }
    }




    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        String[] strings =
                ETFVersionDifferenceHandler.getTextFromTranslation(
                        "config." + ETFClientCommon.MOD_ID + ".player_skin_editor.print_skin.result." + (didSucceed ? "success" : "fail")
                ).getString().split("\n");
        List<Text> lines = new ArrayList<>();

        for (String str :
                strings) {
            lines.add(Text.of(str.trim()));
        }
        int i = 0;
        for (Text txt :
                lines) {
            drawCenteredText(matrices, textRenderer, txt, (int) (width * 0.5), (int) (height * 0.3) + i, 0xFFFFFF);
            i += txt.getString().isEmpty() ? 5 : 10;
        }


    }

}
