# DRLS-PAS-Docker-The Ultimate Guide to Running DRLS PAS for Local Development

## Purpose of this guide

This document details the installation process for the dockerized version of the **Documentation Requirements Lookup Service (DRLS) PAS Workflow** system for Local Development. Be aware that each component of DRLS has its own README where you will find more detailed documentation. This document **is not designed to replace those individual READMEs**. 


This document **is designed to take you through the entire set up process for DRLS using docker containers**. It is a standalone guide that does not depend on any supplementary DRLS documentation.

This guide will take you through the development environment setup for each of the following DRLS components:
1. [Coverage Requirements Discovery (CRD)](https://github.com/HL7-DaVinci/CRD)
2. [(Test) EHR FHIR Service](https://github.com/HL7-DaVinci/test-ehr)
3. [Documents, Templates, and Rules (DTR) SMART on FHIR app](https://github.com/HL7-DaVinci/dtr)
4. [Clinical Decision Support (CDS) Library](https://github.com/HL7-DaVinci/CDS-Library)
5. [CRD Request Generator](https://github.com/HL7-DaVinci/crd-request-generator)
6. [PAS](https://github.com/HL7-DaVinci/PAS.git)
7. Keycloak

### Expected Functionality 
1. File Synchronization between local host system and docker container
2. Automatic Server Reloading whenever source file is changed
    - CRD and prior-auth also reload on CDS_Library changes 
3. Automatic Dependendency Installation whenever package.json, package-lock.json, or build.gradle are changed
4. Automatic Data Loader in test-ehr whenever fhirResourcesToLoad directory is changed

## Table of Contents
- [Prerequisites](#prerequisites)
- [Install core tools](#install-core-tools)
    * [Installing core tools on MacOS](#installing-core-tools-on-macos)
        + [Install Docker Desktop for Mac](#install-docker-desktop-for-mac)
        + [Install Ruby](#install-ruby)
        + [Install Docker-sync](#install-docker-sync)
- [Clone DRLS PAS](#clone-drls-PAS)
- [Configure DRLS PAS](#configure-drls-PAS)
    * [CRD configs](#crd-configs)
    * [test-ehr configs](#test-ehr-configs)
    * [crd-request-generator configs](#crd-request-generator-configs)
    * [dtr configs](#dtr-configs)
    * [PAS configs](#PAS-configs)

    * [Add VSAC credentials to your development environment](#add-vsac-credentials-to-your-development-environment)
- [Run DRLS PAS](#run-drls)
    * [Start Docker Sync](#start-docker-sync-application)
    * [Stop Docker Sync](#stop-docker-sync-application-and-remove-all-containers/volumes)
    * [Useful Docker Sync Commands](#useful-docker-sync-commands)
- [Verify DRLS is working](#verify-drls-is-working)


## Prerequisites

Your computer must have these minimum requirements: 
- x86_64 (64-bit) or equivalent processor
    * Follow these instructions to verify your machine's compliance: https://www.macobserver.com/tips/how-to/mac-32-bit-64-bit/ 
- At least 8 GB of RAM
- At least 256 GB of storage
- Internet access
- [Chrome browser](https://www.google.com/chrome/)
- [Git installed](https://www.atlassian.com/git/tutorials/install-git)

Additionally, you must have credentials (api key) access for the **[Value Set Authority Center (VSAC)](https://vsac.nlm.nih.gov/)**. Later on you will add these credentials to your development environment, as they are required for allowing DRLS to pull down updates to value sets that are housed in VSAC. If you don't already have VSAC credentials, you should [create them using UMLS](https://www.nlm.nih.gov/research/umls/index.html).

### Setting Environment Variables and System Path

How you set environment and path variables may vary depending on your operating system and terminal used. For instance, for zsh on MacOS you typically need to modify .zshrc instead of .bash_profile. To figure out how to set environment variables for your system, consult the guides below or google `how to permentaly set environment/path variables on [insert operating system] [insert terminal type]`.

    For more information on how to set environment variables consult these following guides:

    - https://chlee.co/how-to-setup-environment-variables-for-windows-mac-and-linux/
    - https://www3.ntu.edu.sg/home/ehchua/programming/howto/Environment_Variables.html
    - https://unix.stackexchange.com/questions/117467/how-to-permanently-set-environmental-variables

## Install core tools

### Installing core tools on MacOS


#### Install Docker Desktop for Mac

1. Download the **stable** version of **[Docker for Mac](https://www.docker.com/products/docker-desktop)** and follow the steps in the installer.
2. Once the installation is complete, you should see a Docker icon on your Mac's menu bar (top of the screen). Click the icon and verify that **Docker Desktop is running.**
3. Configure Docker to have access to enough resources. To do this, open Docker Desktop and select Settings > Resources. 

    **Note: The defaults for memory at 2GB and possibly CPU as well are too low to run the entire DRLS PAS workflow. If not enough resources are provided, you may notice containers unexpectedly crashing and stopping. Exact requirements for these resource values will depend on your machine. That said, as a baseline starting point, the system runs relatively smoothly at 15GB memory and 7 CPU Processors on MITRE issued Mac Devices.**

#### Install Visual Studio Code and Extensions

The recomended IDE for this set up is Visual Studio Code

1. Install Visual Studio Code - https://code.visualstudio.com
2. Install Docker extension - https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-docker

#### Install Ruby 
Note: The default ruby that comes with Mac may not install the right package version for docker-sync, it is reccomended to install ruby with a package manager, this guide uses rbenv. 

Reference: https://github.com/rbenv/rbenv

1. Install rbenv
  ```bash
        brew install rbenv
  ```

2. Initialize rbenv and follow instructions (setting system path troubleshooting: https://stackoverflow.com/questions/10940736/rbenv-not-changing-ruby-version)
  ```bash
        rbenv init
   ```
3. Close Terminal so changes take affect
4. Test rbenv is installed correctly 
  ```bash
        curl -fsSL https://github.com/rbenv/rbenv-installer/raw/main/bin/rbenv-doctor | bash    
  ```
5. Install Ruby 
  ```bash
        rbenv install 2.7.2 
   ```
6. Verify that the system is using the correct ruby verions 
  ```bash
        which ruby   
        /Users/$USER/.rbenv/shims/ruby # Correct

        ....

        which ruby 
        /usr/bin/ruby # Incorrect, using system default ruby. Path not set correctly, reference step 2
   ```

#### Install Docker-sync 

1. Download and Install docker-sync using the following command:
    ```bash
        gem install docker-sync -v 0.7.0
    ```
2. Test that the right version is installed 
    ```bash
        docker-sync -v
        0.7.0  # Correct 

        ...

        docker-sync -v
        0.1.1  # Incorrect, make sure you have ruby installed and are not using the default system ruby 
    ```

    Note: The versioning is important, system default ruby sometimes installs version 0.1.1 if -v tag is not set. The 0.1.1 release will not work for the rest of this guide.

## Clone DRLS PAS

1. Create a root directory for the DRLS development work (we will call this `<drlsroot>` for the remainder of this setup guide). While this step is not required, having a common root for the DRLS components will make things a lot easier down the line. 
    ```bash
    mkdir <drlsroot>
    ```

    `<drlsroot>` will be the base directory into which all the other components will be installed. For example, CRD will be cloned to `<drlsroot>/crd`.

    Note: If you are using a different project structure from the above description, you will need to change the corresponding repo paths in docker-compose-prior-auth-dev.yml, docker-sync.yml, and docker-compose.yml

2. Now clone the DRLS component repositories from Github:
    ```bash
    cd <drlsroot>
    git clone https://github.com/HL7-DaVinci/CRD.git CRD
    git clone https://github.com/HL7-DaVinci/test-ehr test-ehr
    git clone https://github.com/HL7-DaVinci/crd-request-generator crd-request-generator
    git clone https://github.com/HL7-DaVinci/dtr dtr
    git clone https://github.com/HL7-DaVinci/prior-auth.git prior-auth
    git clone https://github.com/HL7-DaVinci/prior-auth-client.git prior-auth-client

    cd <drlsroot>/CRD/server
    git clone https://github.com/HL7-DaVinci/CDS-Library.git CDS-Library

    cd <drlsroot>/prior-auth
    git clone https://github.com/HL7-DaVinci/CDS-Library.git CDS-Library

    # Optional if you have access to the private fhir-x12 converter and fhir-x12-frontend repositories - will require changing docker-compose-prior-auth-dev.yml, docker-sync.yml, and docker-compose.yml in configure steps 

    git clone https://github.com/HL7-DaVinci/fhir-x12.git fhir-x12
    git clone https://github.com/HL7-DaVinci/fhir-x12-frontend.git fhir-x12-frontend
    ```

## Open DRLS PAS as VsCode workspace 

The prior-auth repository contains the **PriorAuth.code-workspace** file, which can be used to open the above project structure as a multi-root VS Code workspace. To open this workspace, select *File* > *Open Workspace from File...* and navigate to <drls-root>/prior-auth/PriorAuth.code-workspace. In this workspace configuration, the CDS-Library embedded within CRD and prior-auth are opened as seperate roots for an easier development experience.

The Source Control Tab can be used to easily track changes during the devlopement process and perform git actions, with each root of the workspace having its own source control header. See: https://code.visualstudio.com/docs/editor/versioncontrol

The Docker Extension for VsCode has useful functionality to aid in the development process using this set up guide. This extension lets you eaily visualize the containers, images, networks, and volumes created by this set up. Clicking on a running container will open up the file structure of the container. Right clicking on a running container will give the option to view container logs (useful to see output from select services), attach a shell instance within the container, attach a Visual Studio Code IDE to the container using remote-containers. See: https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-docker

## Configure DRLS PAS

### CRD configs

***None***


### test-ehr configs

***None***


### crd-request-generator configs

***None***

### dtr configs

***None***

### prior-auth-client configs

***None***

### prior-auth configs

***None***

### fhir-x12 (optional) configs

1. Uncomment out the section for fhir-x12 in docker-compose-prior-auth-dev.yml (service + volume), PriorAuth.code-workspace, and docker-sync.yml in the prior-auth folder

### fhir-x12-frontend (optional) configs

1. Uncomment out the section for fhir-x12-frontend in docker-compose-prior-auth-dev.yml (service + volume), PriorAuth.code-workspace, and docker-sync.yml in the prior-auth folder


### Add VSAC credentials to your development environment

> At this point, you should have credentials to access VSAC. If not, please refer to [Prerequisites](#prerequisites) for how to create these credentials and return here after you have confirmed you can access VSAC.
> To download the full ValueSets, your VSAC account will need to be added to the CMS-DRLS author group on https://vsac.nlm.nih.gov/. You will need to request membership access from an admin. If this is not configured, you will get `org.hl7.davinci.endpoint.vsac.errors.VSACValueSetNotFoundException: ValueSet 2.16.840.1.113762.1.4.1219.62 Not Found` errors.

> While this step is optional, we **highly recommend** that you do it so that DRLS will have the ability to dynamically load value sets from VSAC. 

You can see a list of your pre-existing environment variables on your Mac by running `env` in your Terminal. To add to `env`:
1. Set "VSAC_API_KEY" in the .env file in the PAS Repository

    or 

1. `cd ~/`
2. Open `.bash_profile` and add the following lines at the very bottom:
    ```bash
    export VSAC_API_KEY=vsac_api_key
    ```
3. Save `.bash_profile` and complete the update to `env`: 
    ```bash
    source .bash_profile
    ```

> Be aware that if you have chosen to skip this step, you will be required to manually provide your VSAC credentials at http://localhost:8090/data and hit **Reload Data** every time you want DRLS to use new or updated value sets.

Note: How you set environment and path variables may vary depending on your operating system and terminal used. See [Setting Environment Variables and System Path](#setting-environment-variables-and-system-path) for more information.


### Add Compose Project Name 

You can see a list of your pre-existing environment variables on your Mac by running `env` in your Terminal. To add to `env`:
1. Set "COMPOSE_PROJECT_NAME" as "pas_dev" in the .env file in the PAS Repository 

    or

1. `cd ~/`
2. Open `.bash_profile` and add the following lines at the very bottom:
    ```bash
    export COMPOSE_PROJECT_NAME=pas_dev
    ```
3. Save `.bash_profile` and complete the update to `env`: 
    ```bash
    source .bash_profile
    ```

Note: How you set environment and path variables may vary depending on your operating system and terminal used. See [Setting Environment Variables and System Path](#setting-environment-variables-and-system-path) for more information.



## Run DRLS

### Start docker-sync application 
Note: Initial set up will take several minutes and spin up fans with high resource use, be patient, future boots will be much quicker, quieter, and less resource intensive 

```bash
    docker-sync-stack start # This is the equivalent of running docker-sync start followed by docker-compose up
```

### Debugging docker-sync application
1. Select the Debugger Tab on the left side pannel of VsCode
2. From the drop down menu next to Run and Debug select **Debug All REMS Applications (Docker) (workspace)**. This is a compund debugger that combines all the other docker debuggers for all servers and applications in this workspace.
3. When finished debugging, simply hit the disconnect button to close out all debug sessions
4. **Important**: Make sure to close out the **Launch Chrome in Debug Mode** task that gets open in the VsCode terminal space. This task launches chrome in debug mode in order to debug frontend applications in this workspace. This needs to be closed in order to run the debugger again next time, leaving it open will not properly start the frontend debuggers. 

![Closing Launch Chrome Task](./setup-images/ClosingLaunchChromeTask.png)

### Stop docker-sync application and remove all containers/volumes/images
```bash
    docker-sync-stack clean # This is the equivalent of running docker-sync clean followed by docker-compose down
    docker image prune -a #Remove unused images
    docker volume prune # Remove unused volumes
```

### Rebuilding Images and Containers
```bash
    docker-compose -f docker-compose-dev.yml up --build --force-recreate  [<service_name1> <service_name2> ...]
```

or

```bash
    docker-compose -f docker-compose-dev.yml build --no-cache --pull [<service_name1> <service_name2> ...] 
    docker-compose -f docker-compose-dev.yml up --force-recreate  [<service_name1> <service_name2> ...]
```

```bash

    # Options:
    #   --force-recreate                        Recreate containers even if their configuration and image haven't changed.
    #   --build                                 Build images before starting containers.
    #   --pull                                  Pull published images before building images.
    #   --no-cache                              Do not use cache when building the image.
    #   [<service_name1> <service_name2> ...]   Services to recreate, not specifying any service will rebuild and recreate all services
```

After rebuilding imaages and containers, start docker-sync normally  

```bash
    ctrl + c # Stop running "docker-compose up" command (containers running without sync functionality)
    docker-sync-stack start # If this command fails to run, running a second time usually fixes the issue
```

### Useful docker-sync commands
Reference: https://docker-sync.readthedocs.io/en/latest/getting-started/commands.html

## Verify DRLS is working

### Register the test-ehr

1. Go to http://localhost:3005/register.
    - Client Id: **app-login**
    - Fhir Server (iss): **http://localhost:8080/test-ehr/r4**
2. Click **Submit**

### The fun part: Generate a test request

1. Go to http://localhost:3000/ehr-server/reqgen.
2. Click **Patient Select** button in upper left.
3. Find **William Oster** in the list of patients and click the dropdown menu next to his name.
4. Select **E0470** in the dropdown menu.
5. Click anywhere in the row for William Oster.
6. Click **Submit** at the bottom of the page.
7. After several seconds you should receive a response in the form of two **CDS cards**:
    - **Respiratory Assist Device**
    - **Positive Airway Pressure Device**
8. Select **Order Form** on one of those CDS cards.
9. If you are asked for login credentials, use **alice** for username and **alice** for password.
10. A webpage should open in a new tab, and after a few seconds, a questionnaire should appear.
11. Fill out questionnaire and hit next
12. Submit PAS Request to http://localhost:9015/fhir

Congratulations! DRLS is fully installed and ready for you to use!

## Troubleshooting docker-sync
Reference: https://docker-sync.readthedocs.io/en/latest/troubleshooting/sync-stopping.html
