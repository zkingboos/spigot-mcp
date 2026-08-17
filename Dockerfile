FROM itzg/minecraft-server:java21

# Install curl for downloading plugins
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Download FAWE 2.15.4-SNAPSHOT (from CI - latest build supporting 1.21.4-26.x)
RUN mkdir -p /plugins && \
    curl -sL -o /plugins/FastAsyncWorldEdit.jar 'https://ci.athion.net/job/FastAsyncWorldEdit/lastSuccessfulBuild/artifact/artifacts/FastAsyncWorldEdit-Bukkit-2.15.4-SNAPSHOT-1370.jar'

# Copy our plugin
COPY build/libs/spigot-mcp-1.0-SNAPSHOT.jar /plugins/spigot-mcp-1.0-SNAPSHOT.jar

ENV EULA=TRUE
ENV TYPE=SPIGOT
ENV VERSION=1.21.4
ENV MEMORY=2G
ENV ONLINE_MODE=FALSE
ENV ENABLE_RCON=TRUE
ENV RCON_PASSWORD=mcp-test
ENV RCON_PORT=25575

EXPOSE 25565 25575 8080

VOLUME ["/data"]

# Use the default entrypoint but with our plugins pre-loaded
ENTRYPOINT ["/start"]