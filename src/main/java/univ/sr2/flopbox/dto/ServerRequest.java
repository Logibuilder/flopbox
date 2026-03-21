package univ.sr2.flopbox.dto;


import univ.sr2.flopbox.model.Server;


/**
 * DTO utilisé pour la création, la modification et l'affichage des informations d'un serveur.
 * Permet de masquer l'ID interne de la base de données au client et propose
 * des méthodes utilitaires pour la conversion de/vers l'entité JPA.
 */
public record ServerRequest(
        String alias,
        String host,
        int port
) {

    /**
     * Convertit ce DTO (ServerRequest) en une véritable entité (Server)
     * prête à être sauvegardée dans la base de données.
     */
    public Server toServer() {
        return new Server(0, this.host, this.alias, this.port);
    }

    public static ServerRequest toRequest(Server server) {
        return new ServerRequest(server.getAlias(), server.getHost(), server.getPort());
    }
}