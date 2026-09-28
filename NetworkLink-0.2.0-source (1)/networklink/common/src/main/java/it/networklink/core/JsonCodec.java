package it.networklink.core;

public final class JsonCodec {
    private JsonCodec() {}

    public static String snapshot(NodeSnapshot snapshot) {
        StringBuilder out = new StringBuilder(768);
        out.append('{')
                .append("\"id\":\"").append(escape(snapshot.id())).append("\",")
                .append("\"name\":\"").append(escape(snapshot.name())).append("\",")
                .append("\"platform\":\"").append(snapshot.platform()).append("\",")
                .append("\"online\":").append(snapshot.online()).append(',')
                .append("\"players\":").append(snapshot.players()).append(',')
                .append("\"maintenance\":").append(snapshot.maintenance()).append(',')
                .append("\"maintenanceReason\":\"").append(escape(snapshot.maintenanceReason())).append("\",")
                .append("\"timestamp\":").append(snapshot.timestamp()).append(',')
                .append("\"servers\":[");
        boolean first = true;
        for (ServerSnapshot server : snapshot.servers()) {
            if (!first) out.append(',');
            first = false;
            out.append('{')
                    .append("\"name\":\"").append(escape(server.name())).append("\",")
                    .append("\"address\":\"").append(escape(server.address())).append("\",")
                    .append("\"online\":").append(server.online()).append(',')
                    .append("\"players\":").append(server.players()).append(',')
                    .append("\"maintenance\":").append(server.maintenance()).append(',')
                    .append("\"maintenanceReason\":\"").append(escape(server.maintenanceReason())).append("\"")
                    .append('}');
        }
        return out.append("]}").toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }
}
