# Data-Privacy-and-Security-Project

## Authors

- André Reis - fc58192
- Daniel Nunes - fc58257
- Gonçalo Pinto - fc58178

## Project overview

### Network structure - `Chord`

<img src="READMEFiles/chord-p2p-removebg-preview.png" alt="plot" width="200"/>

De modo a criar um sistema peer-to-peer o mais descentralizado possível e ao mesmo tempo eficiente, foi decidido implementar um sistema Chord permitindo que a busca de dados tenha complexidade $O(\log N)$, onde $N$ é o número de nós. Esta eficácia deve-se ao uso de `Finger Tables` em cada nó de modo a ser possível cortar caminho durante a procura de um nó.

### Internal conection structure - `SSL/TLS`

A ligacao entre nodes e feita atraves de uma conexao SSL/TLS que garante uma comunicacao segura "end to end". Cada user cria uma ligacao com os nodes presentes na sua Finger Table e guardada-as num hashmap de modo a poder utilizar a mesma varias vezes se necessario e assim nao estar a sempre a criar uma nova ligacao.

Antes da ligacao segura ser realizada, criamos uma sem requerimentos de seguranca de modo a criar uma chave de sessao e assim partilhar o certificado de ambos os nos.

### Encryption and Decryption - `AES` & `RSA`

Uma vez que as menssagens passam por nos dentro da rede antes de chegar ao recetor e necessario usar encriptar as mesmas, nao sendo sufeciente o uso de SSL/TLS.

De modo a obter uma ecriptacao "end to end" decidimos enviar a menssagem encriptada por uma chave de sessao e o hash disso assinado com a chave privada do transmissor. 

## Project Requirements

Para este projeto, o grupo decidiu utilizar a linguagem `Java`. Isto implica ter uma instalação do mesmo para conseguir executar o projeto.

Além disso, com base na nossa implementação do sistema Chord, o primeiro utilizador a entrar na rede é denominado como o `default node` e necessita de ter como IP, porta e nome: localhost, 8080 e "Wang", respetivamente. Este requirimento deve-se ao facto de facilitar o encontro com um node presente na network quando um novo se liga. Se este se desconectar a rede permanece funcional mas mais nenhum user se vai poder juntar.

## Project Limitations

- Ip not defined
- ...

## How to run the project