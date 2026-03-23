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
### 2. Run the Simulation

To run the simulation with the default settings and your current directory as a workspace, use:
```bash
  docker run -v "/$(pwd):/work" whse
```

You can also pass arguments directly:
```bash
  docker run -v "/$(pwd):/work" whse \
    --dataDir ./data/6 \
    --router build/router/router-linux-amd64 \
    --eventLogFile ./output/results.log
```

To see all available options, run:
```bash
  docker run -v "/$(pwd):/work" whse --help
```
