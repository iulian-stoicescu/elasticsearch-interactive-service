# elasticsearch-interactive-service

This is an application written in Java 17 and Spring Boot 3.1.0.

It is supposed to be encapsulated in a docker container and then deployed to a kubernetes cluster (see the `Dockerfile`
and `deployment.yaml` files).
It exposes a few endpoints that can be used to communicate with an elasticsearch cluster which is also deployed in
kubernetes (see the `elasticsearch-statefulset.yaml` manifest file)

### How it works (in my case, on a Windows machine):

*Disclaimer: Kubernetes and Elasticsearch are completely new technologies to me, therefore some details that I present
here might be obvious and unnecessary*

Prerequisites: Installed Docker Desktop application and enabled Kubernetes

1. Set up elasticsearch
    - installation details: https://www.elastic.co/guide/en/cloud-on-k8s/current/k8s-deploy-eck.html
    - k8s manifest file details: https://www.elastic.co/guide/en/cloud-on-k8s/current/k8s-deploy-elasticsearch.html
2. Create the `elasticsearch-statefulset.yaml` manifest file
    - there are two resources in that file, one `Elasticsearch` resource (a custom resource to simplify the deployment
      and management of Elasticsearch clusters within Kubernetes); one `Service` resource which is used to expose the
      Elasticsearch cluster to other services or clients within the Kubernetes cluster.
    - host `quickstart-es-http` and port `9200` will be used to communicate with the elasticsearch cluster
    - deploy elasticsearch in k8s:
      ```bash
      kubectl apply -f elasticsearch-statefulset.yaml
      ```
3. Find the username, password and HTTP CA certificate SHA-256 fingerprint
    - a default user named `elastic` is automatically created with the password stored in a Kubernetes secret
    - the password (e.g. `Qn1s83TA9Xz8zp209DIs15Uz` ) can be found with the following command:
      ```bash 
      kubectl get secret quickstart-es-elastic-user -o go-template='{{.data.elastic | base64decode}}'
      ```
    - in Elasticsearch 8.0 and later, security is enabled automatically when you start Elasticsearch for the first time.
      That is why the SHA-256 fingerprint is needed when making requests. To find it:
      ```bash
      # first connect to the pod that was created, so that you are inside the k8s cluster:
      kubectl exec -it quickstart-es-default-0 -- /bin/sh
      # go to the location for the certificate file:
      cd config/http-certs
      # calculate the certificate fingerprint:
      openssl x509 -noout -fingerprint -sha256 -inform pem -in ca.crt
      # remove the colon characters from the response: BFB66563DD4C29AE9968803969E7C629C12C504409558871638A69EBCB95DB6E
      ```
    - all of these values are used when defining the configuration (see `ElasticsearchConfiguration.java`) for
      the `ElasticsearchClient` that is used inside this application to communicate with the elasticsearch cluster
4. Create the `Dockerfile` and `deployment.yaml` manifest file
    - for the docker image that we are building we need jdk 17 and decided to use `openjdk:17-jdk-slim`
    - related to the manifest file: again two resources, one `Deployment` and one `Service` that will expose this
      application to the k8s cluster (so
      we can actually call the API endpoints defined in `Controller.java`)
    - build the image:
      ```bash
      docker build -t elasticsearch-interactive-image .
      ```
    - deploy in k8s:
      ```bash
      kubectl apply -f elasticsearch-statefulset.yaml
      ```
5. The java code:
    - as I said before, this application exposes a few REST API endpoints that are used to communicate with the
      elasticsearch cluster that was presented previously
    - there are 5 endpoints: 2 GET, 1 POST, 1 PUT, 1 DELETE (see `Controller.java`)
    - when running the application for the first time, the POST endpoint should be called first
        - it will persist the initial companies' data (see `companies.json`) and will create the index named `companies`
        - for each company (which is backed by the `Company.java` record) we create an `index operation` and then run a
          single bulk request
        - as can be seen in `HelperService::createCompany` method, initially there are some null
          fields: `phoneNumbers`, `socialMediaLinks` and `addresses`. These fields would be updated eventually by using
          the PUT endpoint
        - this POST endpoint is not really necessary, there are probably better ways for initializing the data inside
          the elasticsearch cluster. I've just decided to do it this way in order to better understand how the Java
          Client API works for elasticsearch
    - the GET endpoints were requested in the assignment
    - the PUT endpoint was created to be called from the other application named `veridion` to update the companies with
      the data extracted from the websites html pages
    - the DELETE endpoint was created just for testing purposes


## More details to be added...