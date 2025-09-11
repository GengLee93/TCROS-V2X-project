# TCROS-V2X-project

本研究以 TCROS（Traffic Control and Roadside Operating System）為核心架構，使用 Eclipse MOSAIC 為模擬平台，針對臺北科技大學周邊交通環境進行緊急車輛優先通行系統之模擬與驗證。隨著城市交通日益壅塞，緊急車輛（如救護車、消防車）在通行過程中常遭遇交通號誌延誤與車流阻礙，導致救援效率下降。為改善此問題，本專題導入車聯網（V2X）技術，模擬 OBU（車載單元）與 RSU（路側單元）間的通訊互動，並依據 MOTC 公布之 C-ITS 標準，模擬 EVA（緊急車輛告警）、RSA（路側告警）等封包格式以達成實驗。

研究內容涵蓋地圖建置、封包設計、訊息傳遞流程、號誌控制策略與通行效率分析。透過 TCROS 平台整合通訊模組與模擬邏輯，本專題不僅驗證緊急車輛優先通行策略的可行性，也建立一套具備動態判斷與號誌復位機制的智慧交通模擬框架，期望為未來城市交通管理與智慧救援系統提供技術參考與實證基礎。

## Build

## IDE Setup

## Code Origin & Permission

This project builds upon source code privately provided by FSJohnNtut (GitHub username), with explicit permission granted for reuse and extension in the TCROS-V2X research project.

The original repository is not publicly accessible, but the base implementation was used as a foundation for further development, including enhancements to emergency vehicle communication logic, RSA broadcasting, and traffic light control mechanisms.

## License & Attribution

This project utilizes [Eclipse MOSAIC](https://eclipse.dev/mosaic/) for vehicular communication and mobility simulation.

Eclipse MOSAIC is published under the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/).

If you use Eclipse MOSAIC for research purposes, please cite the following publication:

> K. Schrab et al., “Modeling an ITS Management Solution for Mixed Highway Traffic with Eclipse MOSAIC.”  
> *IEEE Transactions on Intelligent Transportation Systems*, Vol. 24, No. 6, pp. 6575–6585, June 2023.  
> DOI: [10.1109/TITS.2022.3204174](https://doi.org/10.1109/TITS.2022.3204174)

