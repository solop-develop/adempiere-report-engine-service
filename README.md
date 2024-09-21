# ADempiere Report Engine Service

This project define a new way for consume reports using gRPC inside ADempiere. Currently exists a usefull reporter with a good definition like **Print Format**, This repository don't want replace the current approach, just I try improve this with a new functionality as service with some features like:

- Pagination
- Retrieve Data as readable format gRPC Rest API
- Simplification for query with print format based
- Improve embedded format
- Improve query performance
- Define keys for columns
- Define friendly aoperations like `C = A + B`

https://github.com/adempiere/adempiere-report-engine-service/assets/2333092/1da9d15f-7978-41b1-b1db-5079ce74f250

## Requirements
 
Since the ADempiere dependency is vital for this project is high recommended that the you are sure that of project [adempiere-jwt-token](https://github.com/adempiere/adempiere-jwt-token) is installed and the setup is runned in ADempiere Database.

## Run it from Gradle

```Shell
gradle run --args="resources/env.yaml"
```


## Some Notes

For Token validation is used [JWT](https://www.viralpatel.net/java-create-validate-jwt-token/)

## Run with Docker

```Shell
docker pull openls/adempiere-report-engine-service:alpine
```

### Where is the image?

You can find it from [Docker Hub](https://hub.docker.com/r/openls/adempiere-report-engine-service/tags)

### Minimal Docker Requirements
To use this Docker image you must have your Docker engine version greater than or equal to 3.0.

### Environment variables
 * `DB_TYPE`: Database Type (Supported `Oracle` and `PostgreSQL`). Default `PostgreSQL`.
 * `DB_HOST`: Hostname for data base server. Default: `localhost`.
 * `DB_PORT`: Port used by data base server. Default: `5432`.
 * `DB_NAME`: Database name that Adempiere-Backend will use to connect with the database. Default: `adempiere`.
 * `DB_USER`: Database user that Adempiere-Backend will use to connect with the database. Default: `adempiere`.
 * `DB_PASSWORD`: Database password that Adempiere-Backend will use to connect with the database. Default: `adempiere`.
 * `IDLE_TIMEOUT`: It sets the maximum time a connection can sit around without being used before it gets closed to free up resources. Default: `300`.
 * `MINIMUM_IDLE`: It sets the minimum number of connections that should be kept open and ready to use, even if they're not currently being used. This helps improve performance by reducing the time it takes to get a connection. Default: `1`.
 * `MAXIMUM_POOL_SIZE`: It sets the maximum number of connections that can be open at the same time. This helps prevent the pool from getting too big and using up too much memory. Default: `10`.
 * `CONNECTION_TIMEOUT`: it sets the maximum time HikariCP will wait to get a connection from the pool before giving up and throwing an error. Default: `5000`.
 * `MAXIMUM_LIFETIME`: It sets the maximum amount of time a connection can stay open in the pool before it's automatically closed. This helps keep the pool clean and prevents problems. Default: `6000`.
 * `KEEPALIVE_TIME`: It sets a test query that HikariCP will run on connections to make sure they're still working properly. Default: `360000`.
 * `CONNECTION_TEST_QUERY`: It sets how often HikariCP will check if a connection is still working properly. This helps prevent problems with connections that might become inactive. Default: `SELECT 1`
 * `SERVER_PORT`: Port to access Adempiere-Backend from outside of the container. Default: `50059`.
 * `SERVER_LOG_LEVEL`: Log Level. Default: `WARNING`.
 * `TZ`: (Time Zone) Indicates the time zone to set in the nginx-based container, the default value is `America/Caracas` (UTC -4:00).
 * `SYSTEM_LOGO_URL`: Logo of the main image of the system, shown in the login screen.

You can download the last image from docker hub, just run the follow command:

```Shell
docker run -d -p 50059:50059 --name adempiere-report-engine-service -e DB_HOST="localhost" -e DB_PORT=5432 -e DB_NAME="adempiere" -e DB_USER="adempiere" -e DB_PASSWORD="adempiere" openls/adempiere-report-engine-service:alpine
```

See all images [here](https://hub.docker.com/r/openls/adempiere-report-engine-service)

## Run with Docker Compose

You can also run it with `docker compose` for develop enviroment. Note that this is a easy way for start the service with PostgreSQL and template.

### Requirements

- [Docker Compose v2.16.0 or later](https://docs.docker.com/compose/install/linux/)

```Shell
docker compose version
Docker Compose version v2.16.0
```

## Run it

Just go to `docker-compose` folder and run it

```Shell
cd docker-compose
```

```Shell
docker compose up
```

After run you must change some values to ensure that this service is work:

- Open [ZK](http://localhost:8080/webui/)
- Go to **System Rules** -> **General Rules** -> **Server** -> **App Registration**
- Search by **Value** `AWS3`
- Change the **Host** value `http://<your-ip-here>` by your `IP`, keep the `http://` because is the protocol
![App Registration](docs/setup-s3.png)
- Change the [.env](docker-compose/.env) file the variable `S3_GATEWAY_RS_S3_URL` with your `IP`
![Env File](docs/setup-s3-env.png)
- Down and Up the service
- After it you can test service

All files will be saved inside user `tmp` folder
![Postman Test](docs/s3-tmp-files.png)

### Postman Check

You can test it using postman with [this](docs/adempiere_report_engine.json) definition, also you can look it from [here](https://documenter.getpostman.com/view/18440575/2sA3QtfXC3)

### Some Services

This repo has some services, the follow is a list:

- GET `/v1/report-engine/reports/{report_id}`
  - Query `show_as_rows`: true show all as list instead tree
- GET `/v1/report-engine/reports/views/{print_format_id}`
  - Query `show_as_rows`: true show all as list instead tree
- POST `/v1/report-engine/export/{report_id}/{format}`
  - Parameter `format` only is supported `xlsx`

The default host is `http://0.0.0.0:5555`

- If you want to open files from browser then can open http://localhost:9090/browser/adempiere
- If you want to open ZK then can open http://localhost:8080/webui/

#### Some Curl's

**Get Report**

```Curl
curl --location 'http://0.0.0.0:5555/v1/report-engine/reports/145?show_as_rows=true' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJBRF9DbGllbnRfSUQiOjExLCJBRF9PcmdfSUQiOjAsIkFEX1JvbGVfSUQiOjEwMiwiQURfVXNlcl9JRCI6MTAwLCJNX1dhcmVob3VzZV9JRCI6MCwiQURfTGFuZ3VhZ2UiOiJlbl9VUyIsImlhdCI6MTcxOTk0ODQ1OX0.wNlMg2kqqXVuJj9KTzOHamXFURDRaNGU_rTilemC3zM'

```

**Get View**

```Curl
curl --location --globoff 'http://0.0.0.0:5555/v1/report-engine/views/1000001?filters=[{%22name%22%3A%20%22IsSOTrx%22%2C%20%22operator%22%3A%20%22equal%22%2C%20%22values%22%3A%20%22Y%22}%2C%20{%22name%22%3A%20%22DocStatus%22%2C%20%22operator%22%3A%20%22equal%22%2C%20%22values%22%3A%20%22CO%22}]' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJBRF9DbGllbnRfSUQiOjExLCJBRF9PcmdfSUQiOjAsIkFEX1JvbGVfSUQiOjEwMiwiQURfVXNlcl9JRCI6MTAwLCJNX1dhcmVob3VzZV9JRCI6MCwiQURfTGFuZ3VhZ2UiOiJlbl9VUyIsImlhdCI6MTcxOTk0ODQ1OX0.wNlMg2kqqXVuJj9KTzOHamXFURDRaNGU_rTilemC3zM'

```

**Export Report**

```Curl
curl --location --request POST 'http://0.0.0.0:5555/v1/report-engine/export/145/xlsx' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJBRF9DbGllbnRfSUQiOjExLCJBRF9PcmdfSUQiOjAsIkFEX1JvbGVfSUQiOjEwMiwiQURfVXNlcl9JRCI6MTAwLCJNX1dhcmVob3VzZV9JRCI6MCwiQURfTGFuZ3VhZ2UiOiJlbl9VUyIsImlhdCI6MTcxOTk0ODQ1OX0.wNlMg2kqqXVuJj9KTzOHamXFURDRaNGU_rTilemC3zM'
```

**Get Tmp File**

Note: Replace `report_engine_6003747246856772359.xlsx` by the report name

```Curl
curl --location 'http://0.0.0.0:7878/api/resources/2433705d-bb00-4bcd-af7b-38046a92e157/user/a4bd438e-fb40-11e8-a479-7a0060f0aa01/resource/tmp/report_engine_6003747246856772359.xlsx'
```

### Some Variables

You can change variables editing the `.env` file. Note that this file have a minimal example.

## What else?

Help us to improve this big tool!
