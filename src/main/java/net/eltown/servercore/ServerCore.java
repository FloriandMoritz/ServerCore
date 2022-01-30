package net.eltown.servercore;

import com.google.common.net.HttpHeaders;
import lombok.Getter;
import lombok.SneakyThrows;
import net.eltown.servercore.commands.administrative.*;
import net.eltown.servercore.commands.defaults.PluginsCommand;
import net.eltown.servercore.commands.defaults.SpawnCommand;
import net.eltown.servercore.commands.feature.ChestShopCommand;
import net.eltown.servercore.commands.feature.FriendCommand;
import net.eltown.servercore.commands.feature.QuestCommand;
import net.eltown.servercore.commands.feature.RedeemCommand;
import net.eltown.servercore.commands.guardian.*;
import net.eltown.servercore.commands.teleportation.*;
import net.eltown.servercore.components.api.intern.*;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.shops.ShopRoleplay;
import net.eltown.servercore.components.tinyrabbit.TinyRabbit;
import net.eltown.servercore.listeners.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Getter
public class ServerCore extends JavaPlugin {

    // ELTOWN-6
    private static ServerCore serverCore;

    private TinyRabbit tinyRabbit;

    private String serverName;

    private ChestShopAPI chestShopAPI;
    private CrateAPI crateAPI;
    private FurnaceAPI furnaceAPI;
    private GroupAPI groupAPI;
    private HologramAPI hologramAPI;
    private LevelAPI levelAPI;
    private QuestAPI questAPI;
    private SettingsAPI settingsAPI;
    private SyncAPI syncAPI;
    private TeleportationAPI teleportationAPI;
    private CoreAPI coreAPI;
    private FriendAPI friendAPI;
    private GiftKeyAPI giftKeyAPI;

    private ShopRoleplay shopRoleplay;

    @Override
    public void onLoad() {
        Language.init(this);
    }

    @Override
    public void onEnable() {
        try {
            serverCore = this;
            this.saveDefaultConfig();
            this.loadPlugin();
            this.getLogger().info("§aServerCore erfolgreich initialisiert.");
        } catch (final Exception e) {
            e.printStackTrace();
            this.getLogger().warning("§4Fehler beim Initialisieren des ServerCores.");
        }
    }

    @SneakyThrows
    private void loadPlugin() {
        this.tinyRabbit = new TinyRabbit("localhost", "Core/Server/System[Main]");
        this.tinyRabbit.throwExceptions(true);

        this.serverName = this.getConfig().getString("server-name");

        /*
         * API
         */
        if (this.serverName.equals("server-1")) this.chestShopAPI = new ChestShopAPI(this);
        this.crateAPI = new CrateAPI(this);
        this.furnaceAPI = new FurnaceAPI(this);
        this.groupAPI = new GroupAPI(this);
        this.hologramAPI = new HologramAPI(this);
        this.levelAPI = new LevelAPI(this);
        this.questAPI = new QuestAPI(this);
        this.settingsAPI = new SettingsAPI(this);
        this.syncAPI = new SyncAPI(this);
        this.teleportationAPI = new TeleportationAPI(this);
        this.coreAPI = new CoreAPI(this);
        this.friendAPI = new FriendAPI(this);
        this.giftKeyAPI = new GiftKeyAPI(this);

        /*
         * Listeners
         */
        if (this.serverName.equals("server-1")) this.getServer().getPluginManager().registerEvents(new ChestShopListener(this), this);
        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new FurnaceListener(this), this);
        this.getServer().getPluginManager().registerEvents(new LevelListener(this, this.getConfig().getBoolean("settings.farmxp")), this);
        this.getServer().getPluginManager().registerEvents(new QuestListener(this), this);

        /*
         * Commands
         */
        this.getServer().getCommandMap().register("sys", new FlyCommand(this));
        this.getServer().getCommandMap().register("sys", new GamemodeCommand(this));
        this.getServer().getCommandMap().register("sys", new GiftkeySystemCommand(this));
        this.getServer().getCommandMap().register("sys", new GiveFurnaceCommand(this));
        this.getServer().getCommandMap().register("sys", new HealCommand(this));
        this.getServer().getCommandMap().register("sys", new HologramCommand(this));
        this.getServer().getCommandMap().register("sys", new IdCommand(this));
        this.getServer().getCommandMap().register("sys", new LevelSystemCommand(this));
        this.getServer().getCommandMap().register("sys", new NpcCommand(this));
        this.getServer().getCommandMap().register("sys", new PrintItemCommand(this));
        this.getServer().getCommandMap().register("sys", new QuestSystemCommand(this));
        this.getServer().getCommandMap().register("sys", new SetSpawnCommand(this));
        this.getServer().getCommandMap().register("sys", new SpeedCommand(this));

        this.getServer().getCommandMap().register("sys", new PluginsCommand(this));
        this.getServer().getCommandMap().register("sys", new SpawnCommand(this));

        if (this.serverName.equals("server-1")) this.getServer().getCommandMap().register("sys", new ChestShopCommand(this));
        this.getServer().getCommandMap().register("sys", new FriendCommand(this));
        this.getServer().getCommandMap().register("sys", new QuestCommand(this));
        this.getServer().getCommandMap().register("sys", new RedeemCommand(this));

        this.getServer().getCommandMap().register("sys", new BanCommand(this));
        this.getServer().getCommandMap().register("sys", new BanlogCommand(this));
        this.getServer().getCommandMap().register("sys", new CheckbanCommand(this));
        this.getServer().getCommandMap().register("sys", new CheckmuteCommand(this));
        this.getServer().getCommandMap().register("sys", new MuteCommand(this));
        this.getServer().getCommandMap().register("sys", new MutelogCommand(this));
        this.getServer().getCommandMap().register("sys", new UnbanCommand(this));
        this.getServer().getCommandMap().register("sys", new UnbanlogCommand(this));
        this.getServer().getCommandMap().register("sys", new UnmuteCommand(this));
        this.getServer().getCommandMap().register("sys", new UnmutelogCommand(this));

        this.getServer().getCommandMap().register("sys", new CbCommand(this));
        this.getServer().getCommandMap().register("sys", new FwCommand(this));
        this.getServer().getCommandMap().register("sys", new HomeCommand(this));
        this.getServer().getCommandMap().register("sys", new NtCommand(this));
        this.getServer().getCommandMap().register("sys", new TeleportCommand(this));
        this.getServer().getCommandMap().register("sys", new TpacceptCommand(this));
        this.getServer().getCommandMap().register("sys", new TpaCommand(this));
        this.getServer().getCommandMap().register("sys", new WarpCommand(this));

        /*
         * Other
         */
        this.shopRoleplay = new ShopRoleplay(this);
    }

    @Override
    public void onDisable() {
        try {
            this.getServer().getOnlinePlayers().forEach(e -> {
                this.syncAPI.savePlayer(e);
            });
            this.getLogger().info("§aServerCore erfolgreich entladen.");
        } catch (final Exception e) {
            e.printStackTrace();
            this.getLogger().warning("§4Fehler beim Entladen des ServerCores.");
        }
    }

    public String createId(final int i) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        final StringBuilder stringBuilder = new StringBuilder();
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

    public String createNumberId(final int i) {
        final String chars = "1234567890";
        final StringBuilder stringBuilder = new StringBuilder();
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

    public String createId(final int i, final String prefix) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        final StringBuilder stringBuilder = new StringBuilder(prefix + "-");
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

    public String getDate() {
        final Date now = new Date();
        final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm");
        return dateFormat.format(now);
    }

    public long getDuration(final String unit, final int time) {
        long duration = System.currentTimeMillis();
        return switch (unit) {
            case "m" -> duration + time * 60000L;
            case "h" -> duration + time * 3600000L;
            case "d" -> duration + time * 86400000L;
            case "W" -> duration + time * 604800000L;
            case "M" -> duration + (long) (time * 2.6280E+9);
            case "P" -> -1;
            default -> System.currentTimeMillis();
        };
    }

    public String getRemainingTimeFuture(final long duration) {
        if (duration == -1L) {
            return "Permanent";
        } else {
            final SimpleDateFormat today = new SimpleDateFormat("dd.MM.yyyy");
            today.format(System.currentTimeMillis());
            final SimpleDateFormat future = new SimpleDateFormat("dd.MM.yyyy");
            future.format(duration);
            final long time = future.getCalendar().getTimeInMillis() - today.getCalendar().getTimeInMillis();
            final int days = (int) (time / 86400000L);
            final int hours = (int) (time / 3600000L % 24L);
            final int minutes = (int) (time / 60000L % 60L);
            String day = "Tage";
            if (days == 1) {
                day = "Tag";
            }

            String hour = "Stunden";
            if (hours == 1) {
                hour = "Stunde";
            }

            String minute = "Minuten";
            if (minutes == 1) {
                minute = "Minute";
            }

            if (minutes < 1 && days == 0 && hours == 0) {
                return "Wenige Augenblicke";
            } else if (hours == 0 && days == 0) {
                return minutes + " " + minute;
            } else {
                return days == 0 ? hours + " " + hour + " " + minutes + " " + minute : days + " " + day + " " + hours + " " + hour + " " + minutes + " " + minute;
            }
        }
    }

    private final String getURL = "https://minecraftpocket-servers.com/api/?object=votes&element=claim&key=1UdYRD3CmGvbFu6A8Qs4qtyQZW2vsxV6WK&username=";
    private final String setURL = "https://minecraftpocket-servers.com/api/?action=post&object=votes&element=claim&key=1UdYRD3CmGvbFu6A8Qs4qtyQZW2vsxV6WK&username=";

    public void getVote(final String player, final Consumer<String> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                final CloseableHttpClient httpClient = HttpClients.createDefault();
                final HttpGet request = new HttpGet(this.getURL + player.replace(" ", "%20"));

                request.addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9) Gecko/2008052906 Firefox/3.0");

                final CloseableHttpResponse response = httpClient.execute(request);
                final String code = new BufferedReader(new InputStreamReader(response.getEntity().getContent())).readLine();
                callback.accept(code);

                httpClient.close();
            } catch (final Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    public void setVoted(final String player) {
        CompletableFuture.runAsync(() -> {
            try {
                final CloseableHttpClient httpClient = HttpClients.createDefault();

                final HttpPost send = new HttpPost(this.setURL + player.replace(" ", "%20"));
                send.addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9) Gecko/2008052906 Firefox/3.0");
                httpClient.execute(send);

                httpClient.close();
            } catch (final Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    public boolean canAddItem(final Inventory inventory, final ItemStack itemStack) {
        final AtomicInteger free = new AtomicInteger(0);
        for (int i = 0; i < 36; i++) {
            final ItemStack slot = inventory.getItem(i);
            if (slot == null) {
                free.addAndGet(itemStack.getMaxStackSize());
                return true;
            }
            final ItemStack curr = itemStack.clone();
            final ItemStack add = slot.clone();

            curr.setAmount(1);
            add.setAmount(1);

            if (curr.equals(add)) {
                free.addAndGet(slot.getMaxStackSize() - slot.getAmount());
            }
        }
        return free.get() >= itemStack.getAmount();
    }

    public static ServerCore getServerCore() {
        return serverCore;
    }
}
