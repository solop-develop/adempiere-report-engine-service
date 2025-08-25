FROM eclipse-temurin:17.0.16_8-jdk-noble

LABEL maintainer="ySenih@erpya.com; EdwinBetanc0urt@outlook.com;" \
	description="ADempiere Report Engine gRPC"

# Init ENV with default values
ENV \
	# Server
	SERVER_PORT="50059" \
	SERVER_LOG_LEVEL="WARNING" \
	# Database
	DB_HOST="localhost" \
	DB_PORT="5432" \
	DB_NAME="adempiere" \
	DB_USER="adempiere" \
	DB_PASSWORD="adempiere" \
	DB_TYPE="PostgreSQL" \
    # Connection Pool
	IDLE_TIMEOUT="300" \
	MINIMUM_IDLE="1" \
	MAXIMUM_POOL_SIZE="10" \
	CONNECTION_TIMEOUT="5000" \
	MAXIMUM_LIFETIME="6000" \
	KEEPALIVE_TIME="360000" \
	CONNECTION_TEST_QUERY="\"SELECT 1\"" \
	# System
	JAVA_OPTIONS="\"-Xms64M\" \"-Xmx1512M\"" \
	TZ="America/Caracas"

EXPOSE ${SERVER_PORT}


# Add operative system dependencies
RUN	apt-get update && \
	apt-get install -y --no-install-recommends \
		bash \
		ca-certificates \
		fontconfig \
		fonts-dejavu \
		tzdata && \
	fc-cache -f -v && \
	echo "Set Timezone..." && \
	ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
	echo $TZ > /etc/timezone && \
	# Clean up
    apt-get autoremove -y && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/* \
	rm -rf /tmp/*


WORKDIR /opt/apps/server

# Copy src files
COPY docker/adempiere-report-engine-service /opt/apps/server
COPY docker/env.yaml /opt/apps/server/env.yaml
COPY docker/start.sh /opt/apps/server/start.sh


# Add adempiere as user
RUN addgroup adempiere && \
	adduser --disabled-password --gecos "" --ingroup adempiere --no-create-home adempiere && \
	chown -R adempiere /opt/apps/server/ && \
	chmod +x start.sh

USER adempiere

# Start app
ENTRYPOINT ["sh" , "start.sh"]
