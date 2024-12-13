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

### Group conversations
TODO

### Long Term Storage of Messages

As mensagens enviadas são temporáriamente guardadas localmente. `Só após fechar o nó usando o comando "5" é que as suas mensagens serã guardadas remotamente`.
As mensagens são guardadas usando a base de dados MongoDB.

Todas as mensagens guardadas estão encriptadas individualmente usando Shamir Secret Sharing e apenas são desencriptadas quando o utilizador pede para as ver. Assim sendo cada mensagem individual tem várias shares que são necessárias para a reconstruir.
Um utilizador apenas consegue ver as mensagens que enviou ou recebeu e nunca mensagens de outros.

#### Limitações sobre Long-Term Storage
Este sistema de storage NÃO É COMPATIVEL com as mensagens de grupo.
Para efeitos de demonstração foi apenas usada uma só base de dados assim sendo, qualquer atacante que tenha acesso a essa única base de dados poderá reconstruir as mensagens.


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

Quando for pedido que escolha um porto, é necessário escolher portos com um minimo de 2 de distancia entre nós (ex: nó1: 8080, nó2: 8082, nó3 8084).