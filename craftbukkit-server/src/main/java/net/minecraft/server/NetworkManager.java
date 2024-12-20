package net.minecraft.server;

import java.net.SocketAddress;
import java.util.Queue;
import javax.crypto.SecretKey;
import com.cobelpvp.CobelPvPThread;
import com.cobelpvp.CobelSpigot;
import com.cobelpvp.handler.PacketHandler;
import net.minecraft.util.com.google.common.collect.Queues;
import net.minecraft.util.com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.util.com.mojang.authlib.properties.Property;
import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.util.io.netty.channel.local.LocalChannel;
import net.minecraft.util.io.netty.channel.local.LocalServerChannel;
import net.minecraft.util.io.netty.channel.nio.NioEventLoopGroup;
import net.minecraft.util.io.netty.handler.timeout.TimeoutException;
import net.minecraft.util.io.netty.util.AttributeKey;
import net.minecraft.util.io.netty.util.concurrent.Future;
import net.minecraft.util.io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.util.org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
// Spigot start
import com.google.common.collect.ImmutableSet;
import org.spigotmc.SpigotCompressor;
import org.spigotmc.SpigotDecompressor;
// Spigot end

// CobelPvP start
import org.spigotmc.CustomTimingsHandler;
import org.bukkit.craftbukkit.SpigotTimings;
// CobelPvP end

public class NetworkManager extends SimpleChannelInboundHandler {

    private static final Logger i = LogManager.getLogger();
    public static final Marker a = MarkerManager.getMarker("NETWORK");
    public static final Marker b = MarkerManager.getMarker("NETWORK_PACKETS", a);
    public static final Marker c = MarkerManager.getMarker("NETWORK_STAT", a);
    public static final AttributeKey d = new AttributeKey("protocol");
    public static final AttributeKey e = new AttributeKey("receivable_packets");
    public static final AttributeKey f = new AttributeKey("sendable_packets");
    public static final NioEventLoopGroup g = new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
    public static final NetworkStatistics h = new NetworkStatistics();
    private final boolean j;
    private final Queue k = Queues.newConcurrentLinkedQueue();
    // private final Queue l = Queues.newConcurrentLinkedQueue(); // CobelPvP
    public Channel m;
    // Spigot Start
    public SocketAddress n;
    public java.util.UUID spoofedUUID;
    public Property[] spoofedProfile;
    public boolean preparing = true;
    // Spigot End
    private PacketListener o;
    private EnumProtocol p;
    private IChatBaseComponent q;
    private boolean r;
    // Spigot Start
    public static final AttributeKey<Integer> protocolVersion = new AttributeKey<Integer>("protocol_version");
    public static final ImmutableSet<Integer> SUPPORTED_VERSIONS = ImmutableSet.of(4, 5, 47);
    public static final int CURRENT_VERSION = 5;
    public static int getVersion(Channel attr)
    {
        Integer ver = attr.attr( protocolVersion ).get();
        return ( ver != null ) ? ver : CURRENT_VERSION;
    }
    public int getVersion()
    {
        return getVersion( this.m );
    }
    // Spigot End

    // CobelPvP start
    private boolean lockDownIncomingTraffic = false;

    protected boolean lockDownIncomingTraffic() {
        boolean oldValue = this.lockDownIncomingTraffic;
        this.lockDownIncomingTraffic = true;
        return oldValue;
    }
    // CobelPvP end

    public static final GenericFutureListener[] emptyListenerArray = new GenericFutureListener[0]; // CobelPvP

    public NetworkManager(boolean flag) {
        this.j = flag;
    }

    public void channelActive(ChannelHandlerContext channelhandlercontext) throws Exception { // CraftBukkit - throws Exception
        super.channelActive(channelhandlercontext);
        this.m = channelhandlercontext.channel();
        this.n = this.m.remoteAddress();
        // Spigot Start
        this.preparing = false;
        // Spigot End
        this.a(EnumProtocol.HANDSHAKING);
    }

    public void a(EnumProtocol enumprotocol) {
        this.p = (EnumProtocol) this.m.attr(d).getAndSet(enumprotocol);
        this.m.attr(e).set(enumprotocol.a(this.j));
        this.m.attr(f).set(enumprotocol.b(this.j));
        this.m.config().setAutoRead(true);
        i.debug("Enabled auto read");
    }

    public void channelInactive(ChannelHandlerContext channelhandlercontext) {
        this.close(new ChatMessage("disconnect.endOfStream", new Object[0]));
    }

    public void exceptionCaught(ChannelHandlerContext channelhandlercontext, Throwable throwable) {
        ChatMessage chatmessage;

        if (throwable instanceof TimeoutException) {
            chatmessage = new ChatMessage("disconnect.timeout", new Object[0]);
        } else {
            chatmessage = new ChatMessage("disconnect.genericReason", new Object[] { "Internal Exception: " + throwable});
        }

        this.close(chatmessage);
        if (MinecraftServer.getServer().isDebugging()) throwable.printStackTrace(); // Spigot
    }

    protected void a(ChannelHandlerContext channelhandlercontext, Packet packet) {
        if (this.m.isOpen() && !this.lockDownIncomingTraffic) { // CobelPvP
            if (packet.a()) {
                packet.handle(this.o);
                if (packet instanceof PacketPlayInKeepAlive) {
                    this.k.add(packet);
                }

                if (this.o instanceof PlayerConnection) {
                    try {
                        for (PacketHandler handler : CobelSpigot.INSTANCE.getPacketHandlers()) {
                            handler.handleReceivedPacket((PlayerConnection) this.o, packet);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                this.k.add(packet);
            }
        }
    }

    public void a(PacketListener packetlistener) {
        Validate.notNull(packetlistener, "packetListener", new Object[0]);
        i.debug("Set listener of {} to {}", new Object[] { this, packetlistener});
        this.o = packetlistener;
    }

    public void handle(Packet packet, GenericFutureListener... agenericfuturelistener) {

        if (this.isBlockDigOrPlace(packet)) {
            CobelSpigot.getInstance().getThread().getTickThread().addPacket(packet, this, (net.minecraft.util.io.netty.util.concurrent.GenericFutureListener<? extends Future<? super Void>>[]) agenericfuturelistener);
            return;
        }

        if (this.m != null && this.m.isOpen()) {
            // this.i(); // CobelPvP
            this.b(packet, agenericfuturelistener);
        } else {
            // this.l.add(new QueuedPacket(packet, agenericfuturelistener)); // CobelPvP
        }
    }

    private boolean isBlockDigOrPlace(Packet packet) {
        if (packet instanceof PacketPlayInBlockDig) return true;
        if (packet instanceof PacketPlayInBlockPlace) return true;

        return false;
    }

    private void b(Packet packet, GenericFutureListener[] agenericfuturelistener) {
        EnumProtocol enumprotocol = EnumProtocol.a(packet);
        EnumProtocol enumprotocol1 = (EnumProtocol) this.m.attr(d).get();

        if (enumprotocol1 != enumprotocol) {
            i.debug("Disabled auto read");
            this.m.config().setAutoRead(false);
        }

        if (this.m.eventLoop().inEventLoop()) {
            /* CobelPvP - is done in QueuedProtocolSwitch.execute(..)
            if (enumprotocol != enumprotocol1) {
                this.a(enumprotocol);
            }
            */

            QueuedProtocolSwitch.execute(this, enumprotocol, enumprotocol1, packet, agenericfuturelistener); // CobelPvP
        } else {
            this.m.eventLoop().execute(new QueuedProtocolSwitch(this, enumprotocol, enumprotocol1, packet, agenericfuturelistener));
        }
    }

    // CobelPvP start - remove unneeded packet queue
    /*
    private void i() {
        if (this.m != null && this.m.isOpen()) {
            // PaperSpigot  start - Improve Network Manager packet handling
            QueuedPacket queuedpacket;
            while ((queuedpacket = (QueuedPacket) this.l.poll()) != null) {
                this.b(QueuedPacket.a(queuedpacket), QueuedPacket.b(queuedpacket));
            }
            // PaperSpigot end
        }
    }
    */
    // CobelPvP end

    public void a() {
        // this.i(); // CobelPvP
        EnumProtocol enumprotocol = (EnumProtocol) this.m.attr(d).get();

        if (this.p != enumprotocol) {
            if (this.p != null) {
                this.o.a(this.p, enumprotocol);
            }

            this.p = enumprotocol;
        }

        if (this.o != null) {
            // PaperSpigot start - Improve Network Manager packet handling - Configurable packets per player per tick processing
            Packet packet;
            for (int i = org.github.paperspigot.PaperSpigotConfig.maxPacketsPerPlayer; (packet = (Packet) this.k.poll()) != null && i >= 0; --i) {
                // PaperSpigot end

                // CraftBukkit start
                if (!this.isConnected() || !this.m.config().isAutoRead()) {
                    continue;
                }
                // CraftBukkit end

                // CobelPvP start
                if(this.lockDownIncomingTraffic) {
                    this.k.clear();
                    break;
                }
                // CobelPvP end

                // CobelPvP start
                CustomTimingsHandler packetHandlerTimer = SpigotTimings.getPacketHandlerTimings(packet);
                packetHandlerTimer.startTiming();
                try {
                    packet.handle(this.o);
                } finally {
                    packetHandlerTimer.stopTiming();
                }

                if (this.o instanceof PlayerConnection) {
                    try {
                        for (PacketHandler handler : CobelSpigot.INSTANCE.getPacketHandlers()) {
                            handler.handleReceivedPacket((PlayerConnection) this.o, packet);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // CobelPvP end


            }

            this.o.a();
        }

        this.m.flush();
    }

    public SocketAddress getSocketAddress() {
        return this.n;
    }

    public void close(IChatBaseComponent ichatbasecomponent) {
        // Spigot Start
        this.preparing = false;
        this.k.clear(); // Spigot Update - 20140921a
        // this.l.clear(); // Spigot Update - 20140921a // CobelPvP
        // Spigot End
        if (this.m.isOpen()) {
            this.m.close();
            this.q = ichatbasecomponent;
        }
    }

    public boolean c() {
        return this.m instanceof LocalChannel || this.m instanceof LocalServerChannel;
    }

    public void a(SecretKey secretkey) {
        this.m.pipeline().addBefore("splitter", "decrypt", new PacketDecrypter(MinecraftEncryption.a(2, secretkey)));
        this.m.pipeline().addBefore("prepender", "encrypt", new PacketEncrypter(MinecraftEncryption.a(1, secretkey)));
        this.r = true;
    }

    public boolean isConnected() {
        return this.m != null && this.m.isOpen();
    }

    public PacketListener getPacketListener() {
        return this.o;
    }

    public IChatBaseComponent f() {
        return this.q;
    }

    public void g() {
        this.m.config().setAutoRead(false);
    }

    protected void channelRead0(ChannelHandlerContext channelhandlercontext, Object object) {
        this.a(channelhandlercontext, (Packet) object);
    }

    static Channel a(NetworkManager networkmanager) {
        return networkmanager.m;
    }

    // Spigot Start
    public SocketAddress getRawAddress()
    {
        return this.m.remoteAddress();
    }
    // Spigot End


    // Spigot start - protocol patch
    public void enableCompression() {
        // Fix ProtocolLib compatibility
        if ( m.pipeline().get("protocol_lib_decoder") != null ) {
            m.pipeline().addBefore( "protocol_lib_decoder", "decompress", new SpigotDecompressor() );
        } else {
            m.pipeline().addBefore( "decoder", "decompress", new SpigotDecompressor() );
        }

        m.pipeline().addBefore( "encoder", "compress", new SpigotCompressor() );
    }
    // Spigot end
}
