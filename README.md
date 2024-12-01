# Data-Privacy-and-Security-Project

## Authors

- André Reis - fc58192
- Daniel Nunes - fc58257
- Gonçalo Pinto - fc58178

Faculdade: https://ciencias.ulisboa.pt/

## Project overview

### Network structure - `Chord`

<img src="READMEFiles/chord-p2p-removebg-preview.png" alt="plot" width="200"/>

De modo a criar um sistema peer-to-peer o mais descentralizado possível e ao mesmo tempo eficiente, foi decidido implementar um sistema Chord permitindo que a busca de dados tenha complexidade $O(\log N)$, onde $N$ é o número de nós. Esta eficácia deve-se ao uso de `Finger Tables` em cada nó de modo a ser possível cortar caminho durante a procura de um nó.

### Internal conection structure - `SSL/TLS`

A ligação entre os nodes é feita através de uma conexão SSL/TLS, garantindo uma comunicação segura. Cada utilizador cria uma ligação com os nodes presentes na sua Finger Table e armazena-as num hashmap para reutilizá-las, evitando a criação de uma nova ligação cada vez que necessitar de enviar uma menssagem.

Antes da ligação segura ser estabelecida, criamos uma conexão sem requisitos de segurança para gerar uma chave de sessão e assim partilhar o certificado de ambos os nodes.

### Encryption and Decryption - `AES` & `RSA`

Uma vez que as mensagens passam por nós dentro da rede antes de chegarem ao recetor, é necessário encriptá-las, pois o uso de SSL/TLS não é suficiente.

Para obter uma encriptação "end-to-end", decidimos enviar a mensagem encriptada com uma chave de sessão, acompanhada do hash dela assinado com a chave privada do transmissor. Dessa forma, obtemos confidencialidade, uma vez que a mensagem é cifrada; autenticidade, pela assinatura do transmissor; e integridade, garantida pelo hash da mensagem.

## Project Requirements

Para este projeto, o grupo decidiu utilizar a linguagem `Java`. Isto implica ter uma instalação do mesmo para conseguir executar o projeto.

Além disso, com base na nossa implementação do sistema Chord, o primeiro utilizador a entrar na rede é denominado como o `default node` e necessita de ter como IP, porta e nome: IP onde este foi criado (ou se ainda não existir, escolher o IP da rede do seu computador), 8080 e "Wang", respetivamente. Este requirimento deve-se ao facto de facilitar o encontro com um node presente na network quando um novo se liga. Se este se desconectar a rede permanece funcional mas mais nenhum user se vai poder juntar.

`Para o programa funcionar é necessário ir a src\main\java\psd\group4\client\UserService.java linha 26 e mudar a varivel ipDefault para ter o ip da máquina onde o default node está a correr. Caso contrário o programa não vai funcionar.`

Uma vez que cada nó possui 2 ligações (uma segura e outra insegura), é necessário escolher portas com um espaço de intervalo de 2.

Por fim, como manager de dependencias decidimos utilizar a ferramenta do `Maven`, o que implica ter este instalado de modo a correr o projeto.

## How to run the project

#### Complilar 
Para compilar o projeto use o seguinte comando Mavem:
```bash
mvn clean package
```
#### Correr 
Para correr o ficheiro Jar previamente compilado, use o seguinte comando:
- Windows:
```bash
java -jar .\target\ZapDistribuido-1.0-SNAPSHOT.jar
```
- Linux:
```bash
java -jar ./target/ZapDistribuido-1.0-SNAPSHOT.jar
```