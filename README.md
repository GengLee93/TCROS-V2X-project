# TCROS-V2X-project

## Overview
TCROS-V2X-project aims to simulate and validate emergency vehicle priority strategies in urban traffic environments using TCROS standards and Eclipse MOSAIC.

## Research Background
This study is based on TCROS (Taiwan C-ITS Roadside Open Standards) as the core framework and employs Eclipse MOSAIC as the simulation platform to model and validate an emergency vehicle priority system in the traffic environment surrounding National Taipei University of Technology.

As urban traffic congestion intensifies, emergency vehicles (such as ambulances and fire trucks) often encounter delays caused by traffic signals and heavy traffic flow, which reduces rescue efficiency. To address this issue, this project introduces V2X (Vehicle-to-Everything) technology, simulating communication between OBUs (On-Board Units) and RSUs (Roadside Units). Following the C-ITS standards published by the MOTC, the project designs and verifies packet formats such as EVA (Emergency Vehicle Alert) and RSA (Roadside Alert).

Through the integration of communication modules and simulation logic on the TCROS platform, this study validates the feasibility of emergency vehicle priority strategies.

## Simulation Content
- Map construction
- Packet design
- Message transmission flow
- Traffic signal control strategy
- Efficiency analysis

## Project Structure 
```
TCROS-V2X-project/
├── OSM/                   # Map data (NTUT → NTU Hospital ER corridor)
│
├── TcrosApplications/     # Developed V2X applications
│
├── GeneralApplications/   # Applications implemented on Eclipse MOSAIC platform
│
├── TcrosProtocol/         # Packet objects based on 2024 TCROS standards
│
├── Util/                  # Utility functions (packet conversion, simulation parameters)
│
├── secnario/              # Packaged simulation scenarios (traffic flows, events, node configs)
│
├── README.md              # Project documentation
│
└── .idea/.DS_Store        # IDE and environment settings (can be ignored)
```

## License & Attribution

This project utilizes [Eclipse MOSAIC](https://eclipse.dev/mosaic/) for vehicular communication and mobility simulation.

Eclipse MOSAIC is published under the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/).

If you use Eclipse MOSAIC for research purposes, please cite the following publication:

> K. Schrab et al., “Modeling an ITS Management Solution for Mixed Highway Traffic with Eclipse MOSAIC.”  
> *IEEE Transactions on Intelligent Transportation Systems*, Vol. 24, No. 6, pp. 6575–6585, June 2023.  
> DOI: [10.1109/TITS.2022.3204174](https://doi.org/10.1109/TITS.2022.3204174)**

