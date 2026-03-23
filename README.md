# Custom Warehouse Simulator | Boozt Hackaton 2026

## Run UI

1. cd into project directory
    ```bash
    cd simulator_ui
    ```

2. Build docker image
    ```bash
    docker build -t whse-ui .
    ```

3. Run image
    ```bash
    docker run -p 3000:3000 whse-ui
    ```

4. Launch the webapp by going to [http://localhost:3000](http://localhost:3000)
