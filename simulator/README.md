## How to save the image

### 1. Build
```bash
  docker build -t whse .
```

## 2. Save
```bash
  docker save -o whse.tar whse
```

## How to load

### 1. Load the Image
If you received a .tar file, first load the image into your local Docker registry:
```bash
  docker load -i whse.tar
```
### 2. Run the Simulation (Standard)
To run the simulation with the default settings and save the logs to your current folder, use the following command in Git Bash
```bash
  docker run -v "/$(pwd):/app/output" whse
```

### 3. Run with Custom Args
f you need to test specific data directories or configurations, you can pass arguments directly
```bash
  docker run -v "/$(pwd):/app/output" whse \
    --dataDir ./data/6 \
    --router build/router/router-linux-amd64 \
    --eventLogFile ./output/results.log
```

