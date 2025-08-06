## **1. Time-Related Information**
- **timeStamp**: Accumulated value of minutes within the year.
- **second**: Accumulated value of milliseconds within the current minute.
- **sequenceNumber**: Increments by 1 whenever there is a change in message content except for `timeStamp` and `second`. Resets to 0 when the request ends.

## **2. Priority Request**
- **requests**: Priority request message set. A single `SRM` message can contain up to 32 requests.
    - **request**: Contains priority request information for a single intersection.
        - **id**: Intersection ID message set, providing intersection location information and linking with other messages through this set.
            - **region**: Corresponding `MapData` intersection region number, can be user-defined, such as a postal code.
            - **id**: Corresponding `MapData` intersection ID, can be user-defined.
        - **requestID**: Identifier for this request, can be user-defined, corresponding to `SSM request`.
        - **requestType**: Enumeration(0 to 3),Represents the current state of the priority request. The value changes dynamically based on interactions between `SRM` and `SSM`.
            - `priorityRequestTypeReserved (0)`: Reserved value
            - `priorityRequest (1)`: Priority request
            - `priorityRequestUpdate (2)`: Priority request update
            - `priorityCancellation (3)`: Request cancellation
        - **inBoundLane**: Incoming lane for the priority vehicle.
            - **lane**: Corresponding `MapData` lane ID for the incoming vehicle.
        - **outBoundLane**: Outgoing lane for the priority vehicle.
            - **lane**: Corresponding `MapData` lane ID for the outgoing vehicle.

## **3. Estimated Time of Arrival (ETA)**
- **minute**: Estimated arrival time (ETA) in minutes, representing accumulated minutes within the year. Corresponds to `SSM minute`.
- **second**: Estimated arrival time (ETA) in seconds, representing accumulated milliseconds within the current minute. Corresponds to `SSM second`.
- **duration**: Possible extension window for the estimated arrival time (time window). This allows `ETA` to be represented as a time range, reducing the need for frequent `ETA` adjustments. Expressed in milliseconds. Corresponds to `SSM duration`.

## **4. Priority Requestor Information**
- **requestor**: Description of the priority requestor, including related information.
    - **id**: Identifier of the requesting vehicle.
    - **entityID**: ID of the requesting vehicle, can be user-defined. Corresponds to `SSM entityID`.
    - **type**: Includes requestor-related information such as `role`, `request`, and `hpmsType`.
        - **role**: Determines if the requestor is eligible to make a priority request, such as emergency vehicles or public transportation.
            - `basicVehicle (0)`: Passenger car
            - `fire (13)`: Fire truck
            - `ambulance (14)`: Ambulance
            - `transit (16)`: Public transit vehicle
        - **request**: Importance level of the priority request, used for decision-making in case of multiple priority conflicts.
            - `requestImportanceLevelUnKnown (0)`: Importance level unknown
            - `requestImportanceLevel1 (1)`: Lowest importance level
            - `...` Importance increases with value `(2-13)`
            - `requestImportanceLevel14 (14)`: Highest importance level
            - `requestImportanceLevel15 (15)`: Reserved value
        - **hpmsType**: Vehicle type of the priority requestor. For vehicles other than passenger cars and buses, it is recommended to use `0`.
            - `none (0)`: Information unavailable
            - `car (4)`: Passenger car
            - `bus (6)`: Bus

## **5. Priority Requestor Position**
- **position**: Coordinates of the priority requestor, including `long`, `lat`, and `elevation`.
    - **lat**: Latitude, represented in 10 microdegrees. `900000001` indicates no latitude information.
    - **long**: Longitude, represented in 10 microdegrees. `1800000001` indicates no longitude information.
    - **elevation**: Elevation, represented in 10 cm units.

## **6. Public Transit Information**
- **transitStatus**: Special status of public transportation, indicating the vehicle’s operational state, such as door open, stopping, or loading passengers. If data is unavailable, the value is `11111111` (non-public transit vehicle).
    - `loading (0)`: Stopped
    - `anADAuse (1)`: Accessibility service for disabled passengers
    - `aBikeLoad (2)`: Bicycle loading
    - `doorOpen (3)`: Door open for passengers
    - `charging (4)`: Charging
    - `atStopLine (5)`: At stop line
- **transitOccupancy**: Vehicle occupancy level, indicating passenger load. For non-public transit vehicles, the value is `0`.
    - `occupancyUnknown (0)`: Status unknown
    - `occupancyEmpty (1)`: Empty
    - `occupancyVeryLow (2)`: Very low occupancy
    - `occupancyLow (3)`: Low occupancy
    - `occupancyMed (4)`: Medium occupancy
    - `occupancyHigh (5)`: High occupancy
    - `occupancyNearlyFull (6)`: Nearly full
    - `occupancyFull (7)`: Full
- **transitSchedule**: Schedule adherence, indicating whether the public transit vehicle is on time, delayed, or ahead of schedule. For non-public transit vehicles, the value is `-122`.
    - `INTEGER (-122 .. 121)`: Represents schedule deviation in units of 10 seconds, covering a range of ±20 minutes.
    - `Positive values indicate ahead of schedule, negative values indicate delay.`
    - `-121 and 120`: Indicate schedule deviation beyond the defined range.
    - `-122`: Indicates data unavailable.