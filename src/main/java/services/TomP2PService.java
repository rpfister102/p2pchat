package services;

import dtos.UserDTO;
import models.User;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;
import util.ChatLogger;

import java.io.IOException;
import java.net.InetAddress;

public class TomP2PService implements P2PService {
    private User user;
    private PeerDHT peerDHT;

    public TomP2PService(User user) {
        this.user = user;
    }

    @Override
    public void start() throws IOException, InterruptedException {
        Number160 uniqueNodeID = Number160.createHash(user.getUniqueID().toString());
        peerDHT = new PeerBuilderDHT(new PeerBuilder(uniqueNodeID).ports(user.getPort()).start()).start();
        ChatLogger.info("Peer started at port " + user.getPort());

        if (user.hasBootstrapPeer()) {
            ChatLogger.info(String.format(
                    "Start bootstrapping using the peer at %s %d", user.getBootstrapIP(), user.getBootstrapPort()));
            peerDHT.peer()
                    .bootstrap()
                    .inetAddress(InetAddress.getByName(user.getBootstrapIP()))
                    .ports(user.getBootstrapPort())
                    .start()
                    .awaitListeners();
            ChatLogger.info("Bootstrapping completed");
        }

        updateUserInfo();
    }

    private void updateUserInfo() throws IOException {
        UserDTO userDTO = new UserDTO(
                user.getUsername(),
                peerDHT.peerAddress().inetAddress().getHostAddress(),
                user.getPort());

        // todo on collision use domain key, e.g. UUID
        peerDHT.put(Number160.createHash(user.getUsername()))
                .data(new Data(userDTO))
                .start()
                .awaitUninterruptibly();
        ChatLogger.info("User info is updated on DHT: " + userDTO);
    }

    @Override
    public FutureGet getUser(String username) {
        return peerDHT.get(Number160.createHash(username))
                .start();
    }
}
