<h1>GeoFlow</h1>
An opinionated application for creating and running pipelines that load and report location based data.

The overall goal of the project is to create a flexible and extensible application for piping location based data into
an easily traversed and expanded data warehouse. The idea for this project comes from a combination of an application I
used and [Apache Airflow](https://airflow.apache.org/). Airflow didn't quite meet my requirements when considering the
consistency and structure of the incoming data (a lot of the expected data is quite volatile and poorly structured)
while the other custom application was a little rigid and missing opportunities to automate operations.

The application is made up of 3 basic modules:
1. Core operations that span across other modules (ie database operations, tasks, etc.)
2. Worker that handles the running of scheduled jobs (similar to
[Celery](https://docs.celeryproject.org/en/stable/index.html) but with a thread pool and coroutines)
3. Server for user interactions with the underlining service as well as administrative operations

The application is structured using some core building blocks. These include:
1. Data Sources - describes where and how data is collected and reported
2. Workflow Operations - intermediary steps in a workflow that run a generic pipeline specified by the data source
3. Tasks - abstract class with a *runTask* function to implement task logic (considering change to functional
   approach with annotations)
4. Generic Tasks - logic intended to be part of a pipeline but does not hold state or reference to any pipeline until
called
5. Generic Pipelines - ordered collection of concrete tasks used as a template to describe how a workflow operation is
to be run
6. Pipeline Runs - concrete instance of a generic pipeline whose tasks have state and whose collection of tasks can
change

<h3>Wiki</h3>
coming soon....

<h3>Database Diagram</h3>

![db_diagram](https://i.imgur.com/HikIP2h.png)