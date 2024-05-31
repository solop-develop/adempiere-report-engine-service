# ADempiere Report Engine Service

This project define a new way for consume reports using gRPC inside ADempiere. Currently exists a usefull reporter with a good definition like **Print Format**, This repository don't want replace the current approach, just I try improve this with a new functionality as service with some features like:

- Pagination
- Retrieve Data as readable format gRPC Rest API
- Simplification for query with print format based
- Improve embedded format
- Improve query performance
- Define keys for columns
- Define friendly aoperations like `C = A + B`

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
- `DB_TYPE`: Database Type (Supported `Oracle` and `PostgreSQL`). Default `PostgreSQL`
- `DB_HOST`: Hostname for data base server. Default: `localhost`
- `DB_PORT`: Port used by data base server. Default: `5432`
- `DB_NAME`: Database name that adempiere-report-engine-service will use to connect with the database. Default: `adempiere`
- `DB_USER`: Database user that adempiere-report-engine-service will use to connect with the database. Default: `adempiere`
- `DB_PASSWORD`: Database password that Adempiere-Backend will use to connect with the database. Default: `adempiere`
- `SERVER_PORT`: Port to access adempiere-report-engine-service from outside of the container. Default: `50059`
- `SERVER_LOG_LEVEL`: Log Level. Default: `WARNING`
- `TZ`: (Time Zone) Indicates the time zone to set in the nginx-based container, the default value is `America/Caracas` (UTC -4:00).

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

### Postman Check

You can test it using postman with [this](docs/adempiere_report_engine.json) definition, also you can look it from [here](https://documenter.getpostman.com/view/18440575/2sA3QtfXC3)

### Some Services

This repo has some services, the follow is a list:

- `/v1/reports/{report_id}`
- `/v1/reportsviews/{print_format_id}`

The default host is `http://0.0.0.0:5555`

#### Some Curl's

```Curl
curl --location 'http://0.0.0.0:5555/v1/report-engine/views/1001918' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJBRF9DbGllbnRfSUQiOjEwMDAwMDAsIkFEX09yZ19JRCI6MTAwMDAyMCwiQURfUm9sZV9JRCI6MTAwMDAwMCwiQURfVXNlcl9JRCI6MTAwMDAxNywiTV9XYXJlaG91c2VfSUQiOjEwMDAwNjksIkFEX0xhbmd1YWdlIjoiZW5fVVMiLCJpYXQiOjE3MTY2NzY1NTR9.O6ua3R-fon2g8PypVcC_4SgOj6pVzkU4t1CgEaliubM'

```

```Curl
curl --location --globoff 'http://0.0.0.0:5555/v1/report-engine/reports/53998?filters=[{%22name%22%3A%20%22C_BPartner_ID%22%2C%20%22operator%22%3A%20%22equal%22%2C%20%22values%22%3A%201001781}%2C%20{%22name%22%3A%20%22DateTo%22%2C%20%22operator%22%3A%20%22equal%22%2C%20%22values%22%3A%20%222024-05-30T00%3A00%3A00.00Z%22}]' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJBRF9DbGllbnRfSUQiOjEwMDAwMDAsIkFEX09yZ19JRCI6MTAwMDAyMCwiQURfUm9sZV9JRCI6MTAwMDAwMCwiQURfVXNlcl9JRCI6MTAwMDAxNywiTV9XYXJlaG91c2VfSUQiOjEwMDAwNjksIkFEX0xhbmd1YWdlIjoiZW5fVVMiLCJpYXQiOjE3MTY2NzY1NTR9.O6ua3R-fon2g8PypVcC_4SgOj6pVzkU4t1CgEaliubM'

```

### Some Variables

You can change variables editing the `.env` file. Note that this file have a minimal example.

## What else?

Help us to improve this big tool!
