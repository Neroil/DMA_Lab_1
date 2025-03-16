# Labo DMA 1 - Protocoles applicatifs mobiles
> DUNANT Guillaume, HÄFFNER Edwin, JUNOD Arthur

## Questions théorique

### 1.5

#### Mesures

| Protocole | Connexion | Compressé | Taille réception [bytes] | Taille décompréssée [bytes] | Temps [ms] |
| --------- | --------- | --------- | ------------------------ | --------------------------- | ---------- |
| ProtoBuf  | CSD       | Non       | 104                      | -                           | 119        |
| ProtoBuf  | CSD       | Oui       | 89                       | 104                         | 85         |
| ProtoBuf  | UMTS      | Non       | 104                      | -                           | 91         |
| ProtoBuf  | UMTS      | Oui       | 89                       | 104                         | 71         |
| ProtoBuf  | NR5G      | Non       | 104                      | -                           | 78         |
| ProtoBuf  | NR5G      | Oui       | 89                       | 104                         | 105        |
| XML       | CSD       | Non       | 2003                     | -                           | 425        |
| XML       | CSD       | Oui       | 586                      | 2003                        | 258        |
| XML       | UMTS      | Non       | 2003                     | -                           | 96         |
| XML       | UMTS      | Oui       | 586                      | 2003                        | 93         |
| XML       | NR5G      | Non       | 2003                     | -                           | 82         |
| XML       | NR5G      | Oui       | 586                      | 2003                        | 85         |
| Json      | CSD       | Non       | 469                      | -                           | 167        |
| Json      | CSD       | Oui       | 201                      | 469                         | 109        |
| Json      | UMTS      | Non       | 469                      | -                           | 82         |
| Json      | UMTS      | Oui       | 201                      | 469                         | 84         |
| Json      | NR5G      | Non       | 469                      | -                           | 79         |
| Json      | NR5G      | Oui       | 201                      | 469                         | 92         |

#### Conclusion

On peut voir qu'en général pour les tailles que l'on utilise compresser ne semlbe pas améliorer de manière significative le temps d'exécution des requêtes. On peut toutefois voir une amélioration notable quand on utilise la compression sur les fichiers XML (donc plus volumineux) avec la connexion 2G/CSD.

Compresser avec ProtoBuf ne semble pas être approprié car on ne gagne pas beaucoup sur la taille de la requête, à l'inverse de XML et Json.

Sinon de manière général les temps que nous avons relevés ne semble pas être très constants et représentatifs car ils fluctuaient beaucoup quand nous répétions certaines requêtes. Cele implique que sur des différence de temps petites, comme nous avons ici, cela peut fausser les comparaisons entre différentes mesures.

### 2.1

Un des points d'amélioration pour l'utilisation mobile de l'API, ce serait de supporter la compression afin de minimiser la quantité de données lors des requêtes. Une autre chose améliorable qui pourrait être utile dans certain cas, ce serait la possibilité de récupérer plusieurs livres ou plusieurs autheurs sans tous les récupérer.

### 3.1

Le token obtenu par `onNewToken` nous permet d'identifier notre appareil vers le serveur FCM backend. Pour Whatsapp, on créerait un token à l'installation de l'application et il serait ensuite envoyé au seveur FCM pour que celui-ci l'enregistre. Ce token pourrait être régénéré s'il a une durée de vie ou dans les cas de réinstallation de Whatsapp. Ensuite, quand on reçoit un nouveau message le serveur FCM pourrait notifier l'utilisateur de sa récéption en envoyant un message au(x) token(s) enregistré(s) pour cet utilisateur. Si l'utilisateur détient plusieurs appareils, on peut simplement stocker tout les tokens différents sous le même utilisateurs et tous les notifier en cas de nouveau message.